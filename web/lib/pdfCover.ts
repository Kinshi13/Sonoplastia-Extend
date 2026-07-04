"use client";

/** Renders a PDF's first page to a JPEG blob, client-side, for use as a cover thumbnail. */
export async function generatePdfCoverBlob(file: File): Promise<Blob | null> {
  const pdfjsLib = await import("pdfjs-dist");
  pdfjsLib.GlobalWorkerOptions.workerSrc = "/pdf.worker.min.mjs";

  const buffer = await file.arrayBuffer();
  const pdf = await pdfjsLib.getDocument({ data: buffer }).promise;
  const page = await pdf.getPage(1);
  const viewport = page.getViewport({ scale: 2 });

  const canvas = document.createElement("canvas");
  canvas.width = viewport.width;
  canvas.height = viewport.height;
  const context = canvas.getContext("2d");
  if (!context) return null;

  await page.render({ canvasContext: context, viewport, canvas }).promise;

  return new Promise((resolve) => canvas.toBlob(resolve, "image/jpeg", 0.85));
}
