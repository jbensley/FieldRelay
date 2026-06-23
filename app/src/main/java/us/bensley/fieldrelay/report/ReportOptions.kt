package us.bensley.fieldrelay.report

data class HailSizeOption(val inches: Double, val label: String)

fun reportDistanceOptions(): List<Double> = listOf(0.25, 0.5, 1.0, 2.0, 5.0)

fun hailSizeOptions(): List<HailSizeOption> = listOf(
    HailSizeOption(0.25, "0.25 in (Pea)"),
    HailSizeOption(0.50, "0.50 in (Half-inch)"),
    HailSizeOption(0.75, "0.75 in (Penny)"),
    HailSizeOption(1.00, "1.00 in (Quarter)"),
    HailSizeOption(1.25, "1.25 in (Half Dollar)"),
    HailSizeOption(1.50, "1.50 in (Ping Pong Ball)"),
    HailSizeOption(1.75, "1.75 in (Golf Ball)"),
    HailSizeOption(2.00, "2.00 in (Hen Egg)"),
    HailSizeOption(2.50, "2.50 in (Tennis Ball)"),
    HailSizeOption(2.75, "2.75 in (Baseball)"),
    HailSizeOption(3.00, "3.00 in (Tea Cup)"),
    HailSizeOption(4.00, "4.00 in (Grapefruit)"),
    HailSizeOption(4.50, "4.50 in (Softball)"),
)
