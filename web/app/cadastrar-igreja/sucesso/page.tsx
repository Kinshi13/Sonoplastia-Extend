import Link from "next/link";

export default function CadastroSucessoPage() {
  return (
    <div className="mx-auto max-w-sm text-center">
      <h1 className="text-2xl font-semibold mb-2">Pagamento recebido!</h1>
      <p className="text-sm text-text-secondary mb-6">
        Sua igreja está sendo ativada - isso costuma levar só alguns segundos. Entre com o e-mail e
        senha que você cadastrou para acessar o painel administrativo.
      </p>
      <Link
        href="/login"
        className="inline-block rounded-full bg-primary px-5 py-2 text-sm font-medium text-white"
      >
        Ir para o login
      </Link>
    </div>
  );
}
