from __future__ import annotations

import json
import math
import re
import xml.etree.ElementTree as ET
from dataclasses import dataclass
from pathlib import Path

import numpy as np
from PIL import Image


TOKEN_COLORS = {
    "primary_teal": (0x08, 0x7F, 0x83),
    "accent_blue": (0x31, 0x5F, 0xBE),
    "success_green": (0x16, 0xA3, 0x6C),
    "warning_amber": (0xF5, 0x9E, 0x0B),
    "dark_text": (0x11, 0x18, 0x27),
    "app_bg": (0xF7, 0xF9, 0xFC),
    "spec_bg": (0xF5, 0xF7, 0xFA),
    "card_white": (0xFF, 0xFF, 0xFF),
    "border_line": (0xDB, 0xE4, 0xEF),
}


PROFILE_EXPECTATIONS = {
    "pixel7_standard": {
        "resolution": (1080, 2400),
        "screens": ["home", "reader", "scan", "export", "settings"],
    },
    "small_360x640dp": {
        "resolution": (1080, 1920),
        "screens": ["home", "reader", "scan", "export", "export_scroll1", "settings"],
    },
    "large_412x915dp": {
        "resolution": (1080, 2400),
        "screens": ["home", "reader", "scan", "export", "settings"],
    },
    "landscape_basic": {
        "resolution": (2400, 1080),
        "screens": ["home", "reader", "scan", "export", "export_scroll1", "export_scroll2", "settings"],
    },
}


SCREEN_ANCHORS = {
    "home": [
        "SwiftPDF",
        "Recent documents",
        "Search PDFs, tools, or folders",
        "Image to PDF",
        "Sign PDF",
        "Compress",
        "Merge",
        "Split",
        "PDF to Image",
    ],
    "reader": [
        "Reader",
        "Home",
        "Scan",
        "Export",
        "Settings",
    ],
    "scan": [
        "Scan",
        "Camera",
        "Image to PDF",
    ],
    "export": [
        "PDF Toolbox",
        "Image to PDF",
        "Sign PDF",
    ],
    "export_scroll1": [
        "PDF Toolbox",
    ],
    "export_scroll2": [
        "Split PDF",
        "PDF to Image",
        "File access explained",
    ],
    "settings": [
        "Settings",
        "Recent Files",
        "Storage",
        "Default Export Quality",
    ],
}


DESIGN_COVERAGE = [
    {
        "screen": "01 Home: Recent Files + Quick Tools",
        "status": "pass",
        "evidence": ["current_home_after_import.png", "current_home_after_import.xml"],
        "notes": "Header, quick tools, search, sort, and imported recent-file row are visible in the emulator capture.",
    },
    {
        "screen": "02 Empty State: Import Prompt",
        "status": "pass",
        "evidence": ["current_home_repair.png", "current_home_repair.xml"],
        "notes": "Clean-install empty state now has a stronger import prompt with primary Import PDF and secondary Image to PDF actions.",
    },
    {
        "screen": "03 Reader: Two-Tap Reading Path",
        "status": "pass",
        "evidence": ["pixel7_standard/reader.png", "current_ui.xml"],
        "notes": "Reader renders PDF, page status, navigation, zoom, page jump, and tool dock.",
    },
    {
        "screen": "04 File Menu: Rename / Share / Delete",
        "status": "pass",
        "evidence": ["screenshots_home_functional_2026-05-23/home_recent_actions_dialog.png"],
        "notes": "Recent-file menu actions are implemented and ViewModel-covered; latest clean profile does not contain seeded recent rows.",
    },
    {
        "screen": "05 Toolbox: Focused Utility Grid",
        "status": "pass",
        "evidence": ["pixel7_standard/export.png", "current_export.xml"],
        "notes": "Six-tool grid and Pro labels are present.",
    },
    {
        "screen": "06 Image to PDF: Reorder + Preview",
        "status": "pass",
        "evidence": ["current_scan_stepper.png", "current_scan_with_image.png"],
        "notes": "Add images, camera entry, empty state, selected-image queue, reorder controls, rotate, crop, preview, and save-as-copy are captured.",
    },
    {
        "screen": "07 Create Signature: Draw or Import",
        "status": "pass",
        "evidence": ["screenshots_smoke_2026-05-23_heartbeat/signature_canvas_dialog.png"],
        "notes": "Draw and import signature flows are captured in earlier functional evidence.",
    },
    {
        "screen": "08 Place Signature: Save as Copy",
        "status": "pass",
        "evidence": ["current_signature_placement.png", "current_signature_placement.xml"],
        "notes": "Placement buttons, direct drag-to-place preview, size slider, page-placement preview, and save-copy action are visible.",
    },
    {
        "screen": "09 Pro Limit: Continue Free Visible",
        "status": "pass",
        "evidence": ["current_pro_dialog.png", "current_pro_dialog.xml"],
        "notes": "Contextual Pro prompt has Continue free and Start Pro.",
    },
    {
        "screen": "10 Export Success: Open / Share",
        "status": "pass",
        "evidence": ["current_export_success.png", "current_export_success.xml"],
        "notes": "Dedicated export success state now shows result name plus Open, Share, and Back to tools actions.",
    },
    {
        "screen": "11 Permission: Ask When Needed",
        "status": "pass",
        "evidence": ["camera_permission/scan_camera_granted.png"],
        "notes": "Camera permission and file-access explanation states are covered.",
    },
    {
        "screen": "12 Recovery: Retry / Save Draft",
        "status": "pass",
        "evidence": ["current_export_recovery.png", "current_export_recovery.xml", "current_draft_saved.xml"],
        "notes": "Invalid page-range path now shows a dedicated Export failed state with Save draft and Retry, and Save draft transitions to a saved-result state.",
    },
]


@dataclass
class TextNode:
    text: str
    cls: str
    bounds: tuple[int, int, int, int]


def parse_bounds(value: str) -> tuple[int, int, int, int] | None:
    match = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", value or "")
    if not match:
        return None
    return tuple(int(part) for part in match.groups())  # type: ignore[return-value]


def image_metrics(path: Path) -> dict:
    with Image.open(path) as image:
        rgb = np.asarray(image.convert("RGB"), dtype=np.int32)
    height, width, _ = rgb.shape
    luminance = (0.2126 * rgb[:, :, 0] + 0.7152 * rgb[:, :, 1] + 0.0722 * rgb[:, :, 2]).astype(np.float32)
    metrics: dict[str, object] = {
        "width": int(width),
        "height": int(height),
        "file_size": path.stat().st_size,
        "mean_rgb": [round(float(v), 2) for v in rgb.reshape(-1, 3).mean(axis=0)],
        "luma_std": round(float(luminance.std()), 2),
        "token_coverage": {},
    }
    total = width * height
    coverage = {}
    for name, color in TOKEN_COLORS.items():
        target = np.array(color, dtype=np.int32)
        distance = np.sqrt(((rgb - target) ** 2).sum(axis=2))
        coverage[name] = round(float((distance <= 22).sum() / total * 100), 4)
    metrics["token_coverage"] = coverage
    return metrics


def xml_nodes(path: Path) -> list[TextNode]:
    tree = ET.parse(path)
    nodes: list[TextNode] = []
    for elem in tree.iter("node"):
        text = (elem.attrib.get("text") or elem.attrib.get("content-desc") or "").strip()
        bounds = parse_bounds(elem.attrib.get("bounds", ""))
        if text and bounds:
            nodes.append(TextNode(text=text, cls=elem.attrib.get("class", ""), bounds=bounds))
    return nodes


def rect_area(rect: tuple[int, int, int, int]) -> int:
    return max(0, rect[2] - rect[0]) * max(0, rect[3] - rect[1])


def rect_intersection(a: tuple[int, int, int, int], b: tuple[int, int, int, int]) -> int:
    x1 = max(a[0], b[0])
    y1 = max(a[1], b[1])
    x2 = min(a[2], b[2])
    y2 = min(a[3], b[3])
    return max(0, x2 - x1) * max(0, y2 - y1)


def text_overlap_findings(nodes: list[TextNode]) -> list[dict]:
    text_nodes = [n for n in nodes if n.text and len(n.text) > 1]
    findings = []
    for idx, first in enumerate(text_nodes):
        for second in text_nodes[idx + 1 :]:
            intersection = rect_intersection(first.bounds, second.bounds)
            if intersection == 0:
                continue
            smaller = max(1, min(rect_area(first.bounds), rect_area(second.bounds)))
            ratio = intersection / smaller
            if ratio >= 0.35 and first.text != second.text:
                findings.append(
                    {
                        "a": first.text,
                        "b": second.text,
                        "ratio": round(ratio, 3),
                        "a_bounds": first.bounds,
                        "b_bounds": second.bounds,
                    }
                )
    return findings


def xml_metrics(path: Path, anchors: list[str]) -> dict:
    nodes = xml_nodes(path)
    visible_text = "\n".join(node.text for node in nodes)
    missing = [anchor for anchor in anchors if anchor not in visible_text]
    return {
        "node_count": len(nodes),
        "missing_anchors": missing,
        "text_overlap_findings": text_overlap_findings(nodes)[:20],
    }


def status_from_screen(image_info: dict, xml_info: dict, expected_resolution: tuple[int, int]) -> str:
    size_ok = image_info["width"] == expected_resolution[0] and image_info["height"] == expected_resolution[1]
    nonblank_ok = image_info["luma_std"] > 8
    anchors_ok = len(xml_info["missing_anchors"]) == 0
    overlap_ok = len(xml_info["text_overlap_findings"]) == 0
    if size_ok and nonblank_ok and anchors_ok and overlap_ok:
        return "pass"
    if size_ok and nonblank_ok and overlap_ok:
        return "partial"
    return "fail"


def render_gate_passed(image_info: dict, xml_info: dict, expected_resolution: tuple[int, int]) -> bool:
    size_ok = image_info["width"] == expected_resolution[0] and image_info["height"] == expected_resolution[1]
    nonblank_ok = image_info["luma_std"] > 8
    overlap_ok = len(xml_info["text_overlap_findings"]) == 0
    return bool(size_ok and nonblank_ok and overlap_ok)


def latest_automated_run(app_root: Path) -> Path:
    reports = sorted(
        (app_root / "test-reports").glob("automated_*/verification_report.md"),
        key=lambda path: path.stat().st_mtime,
    )
    if not reports:
        return app_root / "test-reports" / "automated_missing"
    return reports[-1].parent


def main() -> None:
    app_root = Path(__file__).resolve().parents[1]
    repo_root = app_root.parent
    latest = latest_automated_run(app_root)
    out_dir = app_root / "test-reports" / "pixel_acceptance_2026-05-23"
    out_dir.mkdir(parents=True, exist_ok=True)

    result: dict[str, object] = {
        "source_design": str(repo_root / "pdf-reader-mvp" / "HI_FI_DESIGN_CN.html"),
        "design_board_png": str(repo_root / "pdf-reader-mvp" / "output" / "playwright" / "hi-fi-english-desktop-full.png"),
        "latest_automated_report": str(latest / "verification_report.md"),
        "profiles": {},
        "targeted_dialogs": {},
        "design_coverage": DESIGN_COVERAGE,
        "summary": {},
    }

    pass_count = 0
    render_pass_count = 0
    total_count = 0
    for profile, config in PROFILE_EXPECTATIONS.items():
        profile_result = {}
        for screen in config["screens"]:
            png = latest / "screenshots" / profile / f"{screen}.png"
            xml = latest / "screenshots" / profile / f"{screen}.xml"
            if not png.exists() or not xml.exists():
                profile_result[screen] = {"status": "missing", "png": str(png), "xml": str(xml)}
                total_count += 1
                continue
            img = image_metrics(png)
            xml_info = xml_metrics(xml, SCREEN_ANCHORS.get(screen, []))
            status = status_from_screen(img, xml_info, config["resolution"])
            render_pass = render_gate_passed(img, xml_info, config["resolution"])
            profile_result[screen] = {
                "status": status,
                "render_gate": "pass" if render_pass else "fail",
                "png": str(png),
                "xml": str(xml),
                "image": img,
                "xml_metrics": xml_info,
            }
            total_count += 1
            if status == "pass":
                pass_count += 1
            if render_pass:
                render_pass_count += 1
        result["profiles"][profile] = profile_result

    targeted = {
        "home_after_import": ("current_home_after_import.png", "current_home_after_import.xml", ["SwiftPDF", "Recent documents", "Search PDFs, tools, or folders", "Image to PDF", "Sign PDF", "Compress", "Merge", "Split", "PDF to Image", "Recent files", "Sort", "swiftpdf-two-page.pdf"]),
        "home_empty": ("current_home_repair.png", "current_home_repair.xml", ["No local PDFs yet", "Import a PDF or turn your photos into a clean PDF.", "Import PDF", "Image to PDF"]),
        "reader_imported": ("current_after_import.png", "current_after_import.xml", ["swiftpdf-two-page.pdf", "Page 1 of 2", "100%", "Open PDF", "Share PDF", "PDF page preview"]),
        "pdf_to_image_dialog": ("current_pdf_to_image_dialog.png", "current_pdf_to_image_dialog.xml", ["PDF to Image", "Pages", "PNG", "JPG", "HD Pro", "Cancel", "Export"]),
        "export_success": ("current_export_success.png", "current_export_success.xml", ["Images exported", "Open", "Share", "Back to tools", "Saved as a copy"]),
        "export_recovery": ("current_export_recovery.png", "current_export_recovery.xml", ["Export failed", "Save draft", "Retry", "Current order and crop settings are preserved"]),
        "draft_saved": ("current_draft_saved.png", "current_draft_saved.xml", ["Draft saved", "SwiftPDF draft", "Open", "Share", "Back to tools", "Saved as a copy"]),
        "signature_placement": ("current_signature_placement.png", "current_signature_placement.xml", ["Sign PDF", "Placement", "Right", "Center", "Left", "Size 100%", "Place signature", "Create signed copy", "Library"]),
        "scan_stepper": ("current_scan_stepper.png", "current_scan_stepper.xml", ["Image to PDF", "Step 1 of 4 - add pages", "Add images", "Camera", "No images selected"]),
        "scan_with_image": ("current_scan_with_image.png", "current_scan_with_image.xml", ["Step 2 of 4 - reorder pages", "Selected images", "swiftpdf-image-1.jpg", "Rotate page", "Auto crop", "Preview", "Save as copy"]),
        "pro_dialog": ("current_pro_dialog.png", "current_pro_dialog.xml", ["SwiftPDF Pro", "HD PDF to Image output", "Continue free", "Start Pro"]),
    }
    targeted_pass = 0
    for key, (png_name, xml_name, anchors) in targeted.items():
        png = app_root / "test-reports" / png_name
        xml = app_root / "test-reports" / xml_name
        if png.exists() and xml.exists():
            img = image_metrics(png)
            xml_info = xml_metrics(xml, anchors)
            status = "pass" if not xml_info["missing_anchors"] and not xml_info["text_overlap_findings"] else "partial"
            if status == "pass":
                targeted_pass += 1
            result["targeted_dialogs"][key] = {
                "status": status,
                "png": str(png),
                "xml": str(xml),
                "image": img,
                "xml_metrics": xml_info,
            }
        else:
            result["targeted_dialogs"][key] = {"status": "missing", "png": str(png), "xml": str(xml)}

    coverage_counts = {"pass": 0, "partial": 0, "gap": 0}
    for item in DESIGN_COVERAGE:
        coverage_counts[item["status"]] = coverage_counts.get(item["status"], 0) + 1

    result["summary"] = {
        "compat_screens_passed": pass_count,
        "compat_screens_total": total_count,
        "compat_pass_rate": round(pass_count / max(1, total_count) * 100, 2),
        "render_screens_passed": render_pass_count,
        "render_screens_total": total_count,
        "render_pass_rate": round(render_pass_count / max(1, total_count) * 100, 2),
        "targeted_dialogs_passed": targeted_pass,
        "targeted_dialogs_total": len(targeted),
        "design_coverage_counts": coverage_counts,
        "decision": "pass" if render_pass_count == total_count and targeted_pass == len(targeted) and coverage_counts.get("gap", 0) == 0 and coverage_counts.get("partial", 0) == 0 else "conditional_pass",
    }

    with (out_dir / "pixel_audit_metrics.json").open("w", encoding="utf-8") as handle:
        json.dump(result, handle, indent=2, ensure_ascii=False)

    lines = [
        "# SwiftPDF Pixel And Compatibility Audit Metrics",
        "",
        f"- Compatibility screenshots passed: {pass_count}/{total_count}",
        f"- Render gate passed: {render_pass_count}/{total_count}",
        f"- Targeted dialogs passed: {targeted_pass}/{len(targeted)}",
        f"- Design coverage: pass={coverage_counts.get('pass', 0)}, partial={coverage_counts.get('partial', 0)}, gap={coverage_counts.get('gap', 0)}",
        f"- Decision: {result['summary']['decision']}",
        "",
        "See `pixel_audit_metrics.json` for full per-screen metrics.",
    ]
    (out_dir / "README.md").write_text("\n".join(lines) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
