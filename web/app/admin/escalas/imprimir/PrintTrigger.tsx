"use client";

import { useEffect } from "react";
import { logScalePrintExportAction } from "../export-actions";

/** Logs the export (Parte 10 - auditoria) then opens the browser's print dialog once the page has
 *  rendered - kept as a tiny separate client island so the print page itself stays a server
 *  component (real Supabase data, no client-side fetch waterfall). */
export function PrintTrigger({ month }: { month?: string }) {
  useEffect(() => {
    logScalePrintExportAction(month).finally(() => {
      setTimeout(() => window.print(), 300);
    });
  }, [month]);

  return (
    <div className="mb-4 flex justify-end print:hidden">
      <button
        onClick={() => window.print()}
        className="rounded-full bg-primary px-4 py-2 text-sm font-medium text-white"
      >
        Imprimir / Salvar como PDF
      </button>
    </div>
  );
}
