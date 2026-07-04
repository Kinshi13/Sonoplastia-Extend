"use server";

import Stripe from "stripe";
import { headers } from "next/headers";
import { createClient } from "@/lib/supabase/server";

export type StartSignupResult = { checkoutUrl?: string; error?: string };

const SLUG_PATTERN = /^[a-z0-9]+(-[a-z0-9]+)*$/;

export async function startChurchSignupAction(formData: FormData): Promise<StartSignupResult> {
  const churchName = String(formData.get("church_name") ?? "").trim();
  const slug = String(formData.get("slug") ?? "").trim().toLowerCase();
  const email = String(formData.get("email") ?? "").trim();
  const password = String(formData.get("password") ?? "");

  if (!churchName) return { error: "Informe o nome da igreja." };
  if (!SLUG_PATTERN.test(slug)) {
    return { error: "O código da igreja deve ter só letras minúsculas, números e hífens (ex: igreja-central)." };
  }
  if (!email || !password) return { error: "Informe e-mail e senha." };
  if (password.length < 6) return { error: "A senha precisa ter pelo menos 6 caracteres." };

  const supabase = await createClient();

  // Check slug availability before creating any account, so a taken slug never leaves behind
  // an auth user with nowhere to go.
  const { data: existingChurch } = await supabase.from("churches").select("id").eq("slug", slug).single();
  if (existingChurch) return { error: "Esse código de igreja já está em uso. Escolha outro." };

  const { data: signUpData, error: signUpError } = await supabase.auth.signUp({ email, password });
  if (signUpError) return { error: signUpError.message };
  const userId = signUpData.user?.id;
  if (!userId) return { error: "Não foi possível criar a conta. Tente novamente." };

  const origin = (await headers()).get("origin") ?? "";
  const stripe = new Stripe(process.env.STRIPE_SECRET_KEY ?? "");

  const session = await stripe.checkout.sessions.create({
    mode: "payment",
    payment_method_types: ["card", "pix"],
    line_items: [{ price: process.env.STRIPE_PRICE_ID!, quantity: 1 }],
    client_reference_id: userId,
    metadata: { church_name: churchName, slug },
    success_url: `${origin}/cadastrar-igreja/sucesso`,
    cancel_url: `${origin}/cadastrar-igreja`,
  });

  if (!session.url) return { error: "Não foi possível iniciar o pagamento. Tente novamente." };

  return { checkoutUrl: session.url };
}
