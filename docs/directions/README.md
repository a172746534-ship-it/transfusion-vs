# Three creative directions

All three carry the same content and meet the same fixed thresholds. Colours are verified by
`lab/color_check.py` from `lab/palettes.json` (run it: 0 failures): text ≥ 4.5:1, UI ≥ 3:1, key numbers
≥ 7:1, every pair of urgency colours ΔE ≥ 15 under deuteranopia, protanopia and tritanopia.
Prototypes: `app/src/test/java/app/transfusionvs/directions/`. Test-only; nothing here ships yet.

Common to all: Home shows six beds on one screen, most urgent first, with a live summary. The reaction bar
is always on screen. One primary action sits at the bottom. Transfusion and repositioning are told apart by
icon + the word VS / TURN + one type colour; the drop is never danger red. Night is designed separately: big
blocks go dark with a coloured edge instead of bright fills, and the alarm is near-black.

| | **1 · Ward Card** | **2 · Halo** | **3 · Ward Board** |
|---|---|---|---|
| **Idea** | The paper card, alive. The same navy, red and amber, and its dots on a line become a live rail per bed. Staff already know this language. | Calm until it matters. Each bed is a ring that closes as its check approaches; an overdue bed breaks out of the circle into a stop sign. | A departures board for the ward. The clock time is the headline, one line per bed, a ruler with a marker sliding to the next tick. Readable from across the room. |
| **Signature** | The rail: filled dots done, the line fills toward the next dot | The ring: fills to the next check, weight grows with urgency | The ruler: ticks and a sliding marker under every row |
| **Type** | Red Hat Display Black (numbers, heads) + Red Hat Text. Tabular figures, slashed zero like the card | Figtree Black + Figtree Regular. Tabular figures | JetBrains Mono ExtraBold (times) + Barlow Semi Condensed |
| **Shape** | 14–16 dp rounded cards | 24–28 dp soft tiles | Square blocks, hairlines |
| **Density** | One card per bed (88 dp), six fit | Two-column tiles, six fit | One row per bed (68 dp), most beds fit |

## Urgency language (colour AND shape AND text)

| State | Ward Card | Halo | Ward Board | Text |
|---|---|---|---|---|
| Calm | hollow dot | thin ring | outlined tag | `in 47 min` |
| Due soon | half-filled dot, pill | thick ring + ticks | hatched tag | `SOON · 10 min` |
| Due now | solid dot + outer ring, pill | full heavy ring | solid square tag | `DUE NOW` |
| Overdue | red diamond with !, red card edge | red octagon | whole row inverted, triangle | `OVERDUE +15 min` |

## Palettes (hex)

| Token | Ward Card day | Ward Card night | Halo day | Halo night | Board day | Board night |
|---|---|---|---|---|---|---|
| Background | `#EEF1F6` | `#0A1220` | `#F6F2EA` | `#100E0C` | `#ECECE6` | `#050505` |
| Card | `#FFFFFF` | `#141F33` | `#FFFFFF` | `#1E1A16` | `#FAFAF6` | `#141414` |
| Ink | `#13233F` | `#E8EDF6` | `#12302B` | `#EFE6DA` | `#101010` | `#F0EFE8` |
| Muted | `#4C5870` | `#A5B1C6` | `#4F625B` | `#B5AA9B` | `#505049` | `#ABABA2` |
| Calm | `#2F6FD0` | `#7585A4` | `#0E8075` | `#648AA8` | `#6E6E64` | `#62626A` |
| Due soon | `#D28000` | `#F9DC6E` | `#C28400` | `#F5DE7E` | `#AD8900` | `#F5D90A` |
| Due now | `#13233F` | `#A6C4FF` | `#3B2F8F` | `#C6C2FF` | `#0047D6` | `#7AA7FF` |
| Overdue / danger | `#A80F1C` | `#FF7269` | `#A50E38` | `#FF7A7F` | `#B0001A` | `#FF6A5E` |
| Reaction bar fill | `#A80F1C` | `#4A1518` | `#A50E38` | `#4A1620` | `#B0001A` | `#3D0E0A` |
| Transfusion cue | `#8A5A00` | `#D9A44A` | `#8C4A6B` | `#D79BBB` | `#6A4A00` | `#CFAE5A` |
| Repositioning cue | `#0F6E74` | `#5CC0C4` | `#0F766E` | `#6FC2B5` | `#00626B` | `#5FC3CC` |
| Alarm background | `#13233F` | `#060B14` | `#12302B` | `#0A0806` | `#FFD400` | `#000000` |
| Alarm action | `#F5A623` | `#C98F36` | `#F4D35E` | `#B79A44` | `#101010` | `#B89B00` |
| Worst ΔE (any colour vision) | 23.4 | 15.3 | 20.5 | 15.3 | 18.9 | 17.7 |

## Honest trade-offs

- **Ward Card** feels most familiar and needs no learning. It is the least surprising of the three.
- **Halo** is the calmest at a glance, and the shape change for overdue is the strongest. It shows fewest beds per screen, and rings take the most width at large font sizes.
- **Ward Board** has the biggest clock times and the densest list, so it copes best with many beds. It looks the most industrial, and its full-yellow day alarm is the loudest screen of the three, which may be too much in a quiet ward by day.

Fonts are all SIL OFL, bundled, with no downloads at runtime.
