"use client";

import { useEffect, useState } from "react";
import { Share, X } from "lucide-react";

const DISMISSED_KEY = "ios-install-hint-dismissed";

export function IosInstallHint() {
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    const nav = window.navigator as Navigator & { standalone?: boolean };
    const isStandalone = nav.standalone === true || window.matchMedia("(display-mode: standalone)").matches;
    const isIos = /iphone|ipad|ipod/i.test(nav.userAgent) || (nav.platform === "MacIntel" && nav.maxTouchPoints > 1);
    const dismissed = localStorage.getItem(DISMISSED_KEY) === "1";

    if (isIos && !isStandalone && !dismissed) {
      // One-time check against browser-only APIs (localStorage/navigator) after mount -
      // this is exactly what an effect is for, not a derived-state cascade.
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setVisible(true);
    }
  }, []);

  if (!visible) return null;

  return (
    <div
      className="fixed inset-x-0 bottom-0 z-50 flex items-center gap-3 border-t border-divider bg-surface px-4 py-3 shadow-lg"
      style={{ paddingBottom: "calc(env(safe-area-inset-bottom) + 0.75rem)" }}
    >
      <p className="flex-1 text-sm text-foreground">
        Instale este app: toque em <Share size={14} className="inline -translate-y-0.5" /> e depois em
        <span className="font-medium"> &quot;Adicionar à Tela de Início&quot;</span>.
      </p>
      <button
        onClick={() => {
          localStorage.setItem(DISMISSED_KEY, "1");
          setVisible(false);
        }}
        className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-text-secondary hover:bg-background"
        aria-label="Fechar"
      >
        <X size={16} />
      </button>
    </div>
  );
}
