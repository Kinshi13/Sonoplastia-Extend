"use client";

import { useEffect, useState } from "react";
import { BellRing, BellOff, Smartphone } from "lucide-react";
import { saveWebPushSubscriptionAction, removeWebPushSubscriptionAction } from "@/lib/pushSubscriptions";

type Status =
  | "checking"
  | "unsupported"
  | "ios-needs-install"
  | "not-configured"
  | "default"
  | "denied"
  | "subscribed"
  | "working";

function urlBase64ToUint8Array(base64String: string): Uint8Array {
  const padding = "=".repeat((4 - (base64String.length % 4)) % 4);
  const base64 = (base64String + padding).replace(/-/g, "+").replace(/_/g, "/");
  const rawData = atob(base64);
  return Uint8Array.from([...rawData].map((c) => c.charCodeAt(0)));
}

function isIosSafari(): boolean {
  const isIos = /iPad|iPhone|iPod/.test(navigator.userAgent) && !("MSStream" in window);
  const isStandalone =
    window.matchMedia("(display-mode: standalone)").matches || (navigator as unknown as { standalone?: boolean }).standalone === true;
  return isIos && !isStandalone;
}

/**
 * Notificações (base) - "Ativar lembretes de Música e Louvor". Explicitly opt-in: permission is
 * only ever requested from inside the click handler below, never on page load (Bloco 15: "pedir
 * permissão apenas após clique do usuário"). If `NEXT_PUBLIC_VAPID_PUBLIC_KEY` isn't configured -
 * true today, since no real VAPID key pair has been generated for this project yet - this renders
 * "em preparação" instead of attempting a subscribe that would just fail (Bloco 26: "confirmar que
 * a UI informa 'em preparação'... sem quebrar o site").
 */
export function WorshipNotificationOptIn({ churchId }: { churchId: string }) {
  const [status, setStatus] = useState<Status>("checking");
  const [error, setError] = useState<string | null>(null);
  const vapidPublicKey = process.env.NEXT_PUBLIC_VAPID_PUBLIC_KEY;

  useEffect(() => {
    let cancelled = false;
    async function check() {
      if (!("serviceWorker" in navigator) || !("PushManager" in window) || !("Notification" in window)) {
        if (!cancelled) setStatus("unsupported");
        return;
      }
      if (isIosSafari()) {
        if (!cancelled) setStatus("ios-needs-install");
        return;
      }
      if (!vapidPublicKey) {
        if (!cancelled) setStatus("not-configured");
        return;
      }
      if (Notification.permission === "denied") {
        if (!cancelled) setStatus("denied");
        return;
      }
      try {
        const registration = await navigator.serviceWorker.ready;
        const existing = await registration.pushManager.getSubscription();
        if (!cancelled) setStatus(existing ? "subscribed" : "default");
      } catch {
        if (!cancelled) setStatus("default");
      }
    }
    check();
    return () => {
      cancelled = true;
    };
  }, [vapidPublicKey]);

  async function handleEnable() {
    setError(null);
    setStatus("working");
    try {
      const permission = await Notification.requestPermission();
      if (permission !== "granted") {
        setStatus(permission === "denied" ? "denied" : "default");
        return;
      }
      const registration = await navigator.serviceWorker.ready;
      const subscription = await registration.pushManager.subscribe({
        userVisibleOnly: true,
        applicationServerKey: urlBase64ToUint8Array(vapidPublicKey!) as BufferSource,
      });
      const json = subscription.toJSON();
      const result = await saveWebPushSubscriptionAction({
        churchId,
        endpoint: subscription.endpoint,
        p256dh: json.keys?.p256dh ?? "",
        auth: json.keys?.auth ?? "",
        userAgent: navigator.userAgent,
        platform: navigator.platform,
        topics: ["worship_daily"],
      });
      if (result.error) {
        setError(result.error);
        setStatus("default");
        return;
      }
      setStatus("subscribed");
    } catch {
      setError("Não foi possível ativar os lembretes neste navegador.");
      setStatus("default");
    }
  }

  async function handleDisable() {
    setError(null);
    setStatus("working");
    try {
      const registration = await navigator.serviceWorker.ready;
      const subscription = await registration.pushManager.getSubscription();
      if (subscription) {
        await removeWebPushSubscriptionAction(subscription.endpoint);
        await subscription.unsubscribe();
      }
      setStatus("default");
    } catch {
      setError("Não foi possível remover os lembretes agora.");
      setStatus("subscribed");
    }
  }

  if (status === "checking") return null;

  if (status === "unsupported" || status === "not-configured") {
    return (
      <p className="text-xs text-text-secondary">
        Lembretes de Música e Louvor: em preparação para este navegador.
      </p>
    );
  }

  if (status === "ios-needs-install") {
    return (
      <div className="rounded-lg border border-dashed border-border-soft p-3 flex items-start gap-2.5 text-xs text-text-secondary">
        <Smartphone size={16} className="mt-0.5 shrink-0" />
        <p>
          Para receber lembretes no iPhone, adicione o Escala Church à Tela de Início e abra por esse ícone:
          toque em Compartilhar no Safari → Adicionar à Tela de Início → abra pelo novo ícone → volte aqui e
          ative os lembretes.
        </p>
      </div>
    );
  }

  if (status === "denied") {
    return (
      <p className="text-xs text-text-secondary">
        Notificações bloqueadas para este site. Para ativar, permita notificações nas configurações do navegador.
      </p>
    );
  }

  return (
    <div className="flex flex-col gap-1.5">
      <button
        onClick={status === "subscribed" ? handleDisable : handleEnable}
        disabled={status === "working"}
        className="flex w-fit items-center gap-1.5 rounded-full border border-border-soft px-3 py-1.5 text-xs font-medium text-foreground/80 hover:border-primary hover:text-primary transition-colors disabled:opacity-60"
      >
        {status === "subscribed" ? <BellOff size={13} /> : <BellRing size={13} />}
        {status === "working" ? "Um momento..." : status === "subscribed" ? "Remover lembretes" : "Ativar lembretes de Música e Louvor"}
      </button>
      {error && <p className="text-xs text-error">{error}</p>}
    </div>
  );
}
