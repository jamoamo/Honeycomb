#!/usr/bin/env python3
"""Render per-module JaCoCo coverage as a GitHub Actions job summary table."""

import os
import pathlib
import xml.etree.ElementTree as ET

COUNTERS = ("INSTRUCTION", "BRANCH", "CLASS")
MINIMUMS = {"INSTRUCTION": 0.90, "BRANCH": 0.85, "CLASS": 0.90}


def module_totals(report):
   """Return {counter: (covered, missed)} for the bundle-level counters of a report."""
   totals = {}
   for counter in report.findall("./counter"):
      kind = counter.get("type")
      if kind in COUNTERS:
         totals[kind] = (int(counter.get("covered", 0)), int(counter.get("missed", 0)))
   return totals


def cell(totals, counter):
   if counter not in totals:
      return "n/a"
   covered, missed = totals[counter]
   total = covered + missed
   if total == 0:
      return "n/a"
   ratio = covered / total
   mark = "" if ratio >= MINIMUMS[counter] else " :warning:"
   return f"{ratio:.1%} ({covered}/{total}){mark}"


def main():
   reports = sorted(pathlib.Path(".").glob("**/target/site/jacoco-ut/jacoco.xml"))
   lines = ["## Coverage", ""]

   if not reports:
      lines.append("No JaCoCo reports found.")
   else:
      lines.append("| Module | Instructions | Branches | Classes |")
      lines.append("| --- | --- | --- | --- |")
      for path in reports:
         root = ET.parse(path).getroot()
         name = root.get("name") or str(path)
         name = name.removeprefix("Code Coverage Report - ")
         totals = module_totals(root)
         lines.append(
            f"| {name} | {cell(totals, 'INSTRUCTION')} "
            f"| {cell(totals, 'BRANCH')} | {cell(totals, 'CLASS')} |"
         )
      lines.append("")
      lines.append(
         ":warning: marks a counter below the configured gate "
         "(instructions 90%, branches 85%, classes 90%)."
      )

   text = "\n".join(lines) + "\n"
   summary = os.environ.get("GITHUB_STEP_SUMMARY")
   if summary:
      with open(summary, "a", encoding="utf-8") as handle:
         handle.write(text)
   print(text)


if __name__ == "__main__":
   main()
