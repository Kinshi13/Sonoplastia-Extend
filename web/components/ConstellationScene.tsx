"use client";

import { useEffect, useRef } from "react";

type Star = { x: number; y: number; r: number };
type Glow = { x: number; y: number; r: number; hue: "primary" | "secondary" };

/**
 * Fase 11.5 (Etapa 3+4): the site's shared background scene - distant stars, faint constellation
 * lines, and soft orbital glow, layered behind every page (mounted once in app/layout.tsx) so the
 * site reads as one continuous "sky" instead of each page rolling its own background.
 *
 * Every layer is driven by the same two cheap inputs - page scroll and (desktop only) pointer
 * position - each with its own small, capped amplitude factor, drawn together in one canvas/one
 * rAF loop rather than one DOM layer + listener per effect:
 *   stars 0.03 · lines 0.07 · glow 0.10 · foreground accents 0.14 · content 0 (untouched)
 * Real content never moves - only this decorative canvas does, and only within a few pixels.
 *
 * Mobile/touch: mouse parallax is skipped entirely (`matchMedia("(pointer: fine)")`), scroll
 * drift only. `prefers-reduced-motion`: both are skipped, stars/lines/glow render once at rest.
 * The canvas is paused (rAF loop exited) whenever the tab is hidden.
 */
export function ConstellationScene() {
  const canvasRef = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    const reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    const hasFinePointer = window.matchMedia("(pointer: fine)").matches;
    const isDark = window.matchMedia("(prefers-color-scheme: dark)").matches;
    const starColor = isDark ? "243,244,250" : "92,98,128";
    const lineColor = isDark ? "124,156,255" : "74,95,224";
    const glowPrimary = isDark ? "124,156,255" : "74,95,224";
    const glowSecondary = isDark ? "156,124,255" : "124,92,224";

    let seed = 90210;
    const random = () => {
      seed = (seed * 1103515245 + 12345) & 0x7fffffff;
      return (seed % 10000) / 10000;
    };

    let width = 0;
    let height = 0;
    let stars: Star[] = [];
    let glows: Glow[] = [];
    let scrollY = 0;
    let pointerX = 0; // -1..1
    let pointerY = 0; // -1..1
    let visible = true;

    function layout() {
      width = window.innerWidth;
      height = Math.max(window.innerHeight, document.documentElement.scrollHeight);
      canvas!.style.width = `${window.innerWidth}px`;
      canvas!.style.height = `${window.innerHeight}px`;
      canvas!.width = window.innerWidth * window.devicePixelRatio;
      canvas!.height = window.innerHeight * window.devicePixelRatio;
      ctx!.setTransform(window.devicePixelRatio, 0, 0, window.devicePixelRatio, 0, 0);

      const starCount = Math.min(70, Math.floor((window.innerWidth * window.innerHeight) / 22000));
      stars = Array.from({ length: starCount }, () => ({
        x: random() * width,
        y: random() * height,
        r: random() * 1.3 + 0.4,
      }));
      glows = [
        { x: width * 0.15, y: window.innerHeight * 0.15, r: Math.max(width, 400) * 0.35, hue: "primary" },
        { x: width * 0.88, y: window.innerHeight * 0.55, r: Math.max(width, 400) * 0.28, hue: "secondary" },
      ];
      draw();
    }

    function draw() {
      const viewportTop = reduceMotion ? 0 : scrollY;
      const mx = hasFinePointer && !reduceMotion ? pointerX : 0;
      const my = hasFinePointer && !reduceMotion ? pointerY : 0;

      ctx!.clearRect(0, 0, window.innerWidth, window.innerHeight);

      // Layer: orbital glow - factor 0.10 (scroll), small mouse drift
      for (const glow of glows) {
        const dy = -viewportTop * 0.1 + my * 18;
        const dx = mx * 18;
        const gy = glow.y + dy;
        const grad = ctx!.createRadialGradient(glow.x + dx, gy, 0, glow.x + dx, gy, glow.r);
        const color = glow.hue === "primary" ? glowPrimary : glowSecondary;
        grad.addColorStop(0, `rgba(${color}, 0.10)`);
        grad.addColorStop(1, `rgba(${color}, 0)`);
        ctx!.fillStyle = grad;
        ctx!.fillRect(0, 0, window.innerWidth, window.innerHeight);
      }

      // Layer: distant stars - factor 0.03 (scroll), tiny mouse drift
      const starDy = -viewportTop * 0.03 + my * 4;
      const starDx = mx * 4;
      const visibleStars: { x: number; y: number; r: number }[] = [];
      for (const star of stars) {
        const y = ((star.y + starDy) % height + height) % height;
        if (y > window.innerHeight + 20) continue;
        const point = { x: star.x + starDx, y, r: star.r };
        visibleStars.push(point);
        ctx!.beginPath();
        ctx!.fillStyle = `rgba(${starColor}, 0.35)`;
        ctx!.arc(point.x, point.y, point.r, 0, Math.PI * 2);
        ctx!.fill();
      }

      // Layer: constellation lines - factor 0.07 (scroll+mouse), only between nearby stars so it
      // reads as a constellation, not a spiderweb
      const lineDy = -viewportTop * 0.07 + my * 8;
      const lineDx = mx * 8;
      ctx!.strokeStyle = `rgba(${lineColor}, 0.12)`;
      ctx!.lineWidth = 1;
      for (let i = 0; i < visibleStars.length; i++) {
        for (let j = i + 1; j < visibleStars.length; j++) {
          const a = visibleStars[i];
          const b = visibleStars[j];
          const dist = Math.hypot(a.x - b.x, a.y - b.y);
          if (dist < 140) {
            ctx!.beginPath();
            ctx!.moveTo(a.x + (lineDx - starDx), a.y + (lineDy - starDy));
            ctx!.lineTo(b.x + (lineDx - starDx), b.y + (lineDy - starDy));
            ctx!.stroke();
          }
        }
      }

      // Layer: foreground accent stars - factor 0.14 (scroll+mouse), brighter, comet-tinted
      const accentDy = -viewportTop * 0.14 + my * 26;
      const accentDx = mx * 26;
      for (let i = 0; i < visibleStars.length; i += 7) {
        const point = visibleStars[i];
        ctx!.beginPath();
        ctx!.fillStyle = `rgba(${lineColor}, 0.5)`;
        ctx!.arc(point.x + (accentDx - starDx), point.y + (accentDy - starDy), point.r + 0.6, 0, Math.PI * 2);
        ctx!.fill();
      }
    }

    let ticking = false;
    function requestDraw() {
      if (ticking || !visible) return;
      ticking = true;
      requestAnimationFrame(() => {
        draw();
        ticking = false;
      });
    }

    function onScroll() {
      scrollY = window.scrollY;
      requestDraw();
    }
    function onPointerMove(event: PointerEvent) {
      pointerX = (event.clientX / window.innerWidth) * 2 - 1;
      pointerY = (event.clientY / window.innerHeight) * 2 - 1;
      requestDraw();
    }
    function onVisibilityChange() {
      visible = document.visibilityState === "visible";
      if (visible) requestDraw();
    }

    layout();
    window.addEventListener("resize", layout);
    document.addEventListener("visibilitychange", onVisibilityChange);
    if (!reduceMotion) {
      window.addEventListener("scroll", onScroll, { passive: true });
      if (hasFinePointer) window.addEventListener("pointermove", onPointerMove, { passive: true });
    }

    return () => {
      window.removeEventListener("resize", layout);
      document.removeEventListener("visibilitychange", onVisibilityChange);
      window.removeEventListener("scroll", onScroll);
      window.removeEventListener("pointermove", onPointerMove);
    };
  }, []);

  return <canvas ref={canvasRef} aria-hidden="true" className="pointer-events-none fixed inset-0 -z-10" />;
}
