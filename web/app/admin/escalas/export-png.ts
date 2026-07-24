import { Scale } from "@/lib/types/database";
import { ConstellationKind, constellationLabel } from "@/components/celestial/DayConstellation";

const ROLE_FIELDS: { key: keyof Scale; label: string }[] = [
  { key: "reception_person", label: "Recepção" },
  { key: "sound_person", label: "Sonoplastia" },
  { key: "preaching_person", label: "Pregação" },
  { key: "conducting_person", label: "Regência" },
  { key: "musical_message_person", label: "Mensagem musical" },
];

const KIND_ACCENT: Record<ConstellationKind, string> = {
  BEACON: "#7c9cff",
  CROWN: "#e0b04a",
  DAWN: "#b39cff",
  PILGRIM: "#a98bff",
};

/**
 * Fase 11.8 (Parte 20-21): renders a single scale as a shareable PNG, hand-drawn on a canvas so
 * it visually matches the CelestialCard language (dark gradient ground, accent border, corner
 * star, constellation label) instead of a generic screenshot or a plain white table. No DOM
 * capture library (html2canvas/dom-to-image) was added for this - it's a deliberately small,
 * self-contained draw routine covering the one format this phase ships: the 4:5 share card.
 * 9:16/16:9 variants are not implemented yet (see the Fase 11.8 report for why).
 *
 * Correção/expansão de compartilhamento: `churchName` now comes from the real church row (this
 * used to be hardcoded to the literal string "Escala Church" by the only caller, which meant
 * every exported card printed the product's own name where the church's name belongs). Also
 * added an optional public-link/code footer line and a PNG/JPEG output switch - the drawing
 * itself is unchanged otherwise, still one small routine, no new dependency.
 */
export async function renderScaleCardPng(
  scale: Scale,
  kind: ConstellationKind,
  churchName: string,
  formatDatePt: (d: string) => string,
  formatTimePt: (t: string) => string,
  options?: { publicUrl?: string; churchCode?: string; format?: "png" | "jpeg"; quality?: number }
): Promise<Blob> {
  const width = 1080;
  const height = 1350; // 4:5
  const canvas = document.createElement("canvas");
  canvas.width = width;
  canvas.height = height;
  const ctx = canvas.getContext("2d")!;
  const accent = KIND_ACCENT[kind];

  // Layer 4: gradient ground
  const bg = ctx.createLinearGradient(0, 0, width, height);
  bg.addColorStop(0, "#12162a");
  bg.addColorStop(1, "#090b14");
  ctx.fillStyle = bg;
  ctx.fillRect(0, 0, width, height);

  // Layer 1: accent glow, top-right
  const glow = ctx.createRadialGradient(width * 0.85, height * 0.12, 0, width * 0.85, height * 0.12, 420);
  glow.addColorStop(0, `${accent}33`);
  glow.addColorStop(1, "transparent");
  ctx.fillStyle = glow;
  ctx.fillRect(0, 0, width, height);

  // Layer 2: frame
  ctx.strokeStyle = `${accent}88`;
  ctx.lineWidth = 3;
  roundRectCut(ctx, 40, 40, width - 80, height - 80, 34, 20);
  ctx.stroke();

  // Layer 5: constellation watermark (simple point cluster, not the full SVG path - a lightweight
  // canvas-native echo of the same idea rather than re-parsing SVG path data here)
  ctx.save();
  ctx.globalAlpha = 0.12;
  ctx.fillStyle = accent;
  const wx = width - 220;
  const wy = height - 260;
  [[0, 0], [60, -90], [-40, -70], [90, -30]].forEach(([dx, dy]) => {
    ctx.beginPath();
    ctx.arc(wx + dx, wy + dy, 6, 0, Math.PI * 2);
    ctx.fill();
  });
  ctx.restore();

  // Layer 7: corner star
  drawStar(ctx, 92, 96, 16, accent);

  // Content
  ctx.fillStyle = accent;
  ctx.font = "600 30px system-ui, sans-serif";
  ctx.fillText(constellationLabel(kind).toUpperCase() + "  ·  " + churchName, 90, 190);

  ctx.fillStyle = "#f3f4fa";
  ctx.font = "500 84px Georgia, serif";
  wrapText(ctx, scale.title, 90, 300, width - 180, 92);

  ctx.fillStyle = "#a6abc7";
  ctx.font = "400 34px system-ui, sans-serif";
  const dateLine = `${formatDatePt(scale.date)} às ${formatTimePt(scale.start_time)}${scale.end_time ? " - " + formatTimePt(scale.end_time) : ""}`;
  ctx.fillText(dateLine, 90, 400);

  let y = 480;
  const roles = ROLE_FIELDS.filter(({ key }) => typeof scale[key] === "string" && (scale[key] as string).trim());
  for (const { key, label } of roles) {
    ctx.fillStyle = "#5c6280";
    ctx.font = "400 26px system-ui, sans-serif";
    ctx.fillText(label.toUpperCase(), 90, y);
    ctx.fillStyle = "#f3f4fa";
    ctx.font = "600 38px system-ui, sans-serif";
    ctx.fillText(scale[key] as string, 90, y + 44);
    y += 110;
  }

  if (options?.publicUrl) {
    ctx.fillStyle = "#8a90b3";
    ctx.font = "500 26px system-ui, sans-serif";
    ctx.fillText(options.publicUrl.replace(/^https?:\/\//, ""), 90, height - 140);
  }
  if (options?.churchCode) {
    ctx.fillStyle = "#5c6280";
    ctx.font = "400 22px system-ui, sans-serif";
    ctx.fillText(`Código da igreja: ${options.churchCode}`, 90, height - 105);
  }

  ctx.fillStyle = "#5c6280";
  ctx.font = "400 24px system-ui, sans-serif";
  ctx.fillText("✦ Escala Church", 90, height - 60);

  const mime = options?.format === "jpeg" ? "image/jpeg" : "image/png";
  const quality = options?.format === "jpeg" ? (options.quality ?? 0.92) : undefined;
  return new Promise((resolve) => canvas.toBlob((blob) => resolve(blob!), mime, quality));
}

function roundRectCut(ctx: CanvasRenderingContext2D, x: number, y: number, w: number, h: number, r: number, cut: number) {
  ctx.beginPath();
  ctx.moveTo(x, y + cut);
  ctx.lineTo(x + cut, y);
  ctx.lineTo(x + w - r, y);
  ctx.arcTo(x + w, y, x + w, y + r, r);
  ctx.lineTo(x + w, y + h - r);
  ctx.arcTo(x + w, y + h, x + w - r, y + h, r);
  ctx.lineTo(x, y + h);
  ctx.closePath();
}

function drawStar(ctx: CanvasRenderingContext2D, cx: number, cy: number, r: number, color: string) {
  ctx.save();
  ctx.fillStyle = color;
  ctx.beginPath();
  const inner = r * 0.32;
  const points = [
    [cx, cy - r], [cx + inner, cy - inner], [cx + r, cy], [cx + inner, cy + inner],
    [cx, cy + r], [cx - inner, cy + inner], [cx - r, cy], [cx - inner, cy - inner],
  ];
  points.forEach(([px, py], i) => (i === 0 ? ctx.moveTo(px, py) : ctx.lineTo(px, py)));
  ctx.closePath();
  ctx.fill();
  ctx.restore();
}

function wrapText(ctx: CanvasRenderingContext2D, text: string, x: number, y: number, maxWidth: number, lineHeight: number) {
  const words = text.split(" ");
  let line = "";
  let cy = y;
  for (const word of words) {
    const test = line ? `${line} ${word}` : word;
    if (ctx.measureText(test).width > maxWidth && line) {
      ctx.fillText(line, x, cy);
      line = word;
      cy += lineHeight;
    } else {
      line = test;
    }
  }
  if (line) ctx.fillText(line, x, cy);
}
