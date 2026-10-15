package com.saeed.ideasmo.core.model

/**
 * The seven families of node relations. Grounded in established standards:
 * SKOS (hierarchy / association / equivalence), UML (composition, dependency),
 * causal loop diagrams (causality with polarity), IBIS + Toulmin (argument),
 * and concept mapping (a link is a readable phrase, not just a line).
 *
 * This catalog is the single source of truth: the picker, the board rendering and the
 * Help screen all read from it, so nothing can drift apart.
 */
enum class LinkFamily(val title: String, val question: String) {
    Hierarchy("Hierarchy", "What kind of thing is it?"),
    Composition("Composition", "What is it made of?"),
    Association("Association", "What is it connected to?"),
    Equivalence("Equivalence", "Is it the same thing?"),
    Causality("Causality", "What makes what happen?"),
    Logic("Logic & argument", "What does it imply, support or contradict?"),
    Process("Process & time", "What comes first, what blocks what?"),
}

/** How the line is drawn. */
enum class LinkLine { Solid, Dashed, Dotted, Double }

/** Where arrowheads go. */
enum class LinkArrow { None, Single, Double }

/** Which Material colour role the link uses — never a raw hex. */
enum class LinkColor { Primary, Secondary, Tertiary, Error, Outline }

data class LinkType(
    val key: String,
    val family: LinkFamily,
    /** Short name shown in the picker and as the label on the board. */
    val name: String,
    /** Template used to read the link as a sentence; %1$s = source, %2$s = target. */
    val readAs: String,
    /** A concrete example with real words, for the picker and Help. */
    val example: String,
    /** One line explaining when to use it. */
    val meaning: String,
    val directed: Boolean,
    val line: LinkLine,
    val arrow: LinkArrow,
    val color: LinkColor,
)

object LinkCatalog {

    const val DEFAULT_KEY = "relates"

    val all: List<LinkType> = listOf(
        // ---- 1. Hierarchy -------------------------------------------------
        LinkType(
            "is_a", LinkFamily.Hierarchy, "is a", "%1\$s is a %2\$s",
            "Cat is a Mammal",
            "Class membership: the source belongs to the target's kind.",
            true, LinkLine.Solid, LinkArrow.Single, LinkColor.Primary,
        ),
        LinkType(
            "instance_of", LinkFamily.Hierarchy, "instance of", "%1\$s is an instance of %2\$s",
            "Tehran is an instance of City",
            "The source is one concrete case of the target class.",
            true, LinkLine.Solid, LinkArrow.Single, LinkColor.Primary,
        ),
        LinkType(
            "broader", LinkFamily.Hierarchy, "broader than", "%1\$s is broader than %2\$s",
            "Animal is broader than Cat",
            "The source is the more general term (thesaurus sense).",
            true, LinkLine.Solid, LinkArrow.Single, LinkColor.Secondary,
        ),
        LinkType(
            "narrower", LinkFamily.Hierarchy, "narrower than", "%1\$s is narrower than %2\$s",
            "Cat is narrower than Animal",
            "The source is the more specific term.",
            true, LinkLine.Solid, LinkArrow.Single, LinkColor.Secondary,
        ),
        LinkType(
            "generalizes", LinkFamily.Hierarchy, "generalizes", "%1\$s generalizes %2\$s",
            "Vehicle generalizes Car",
            "The source is the parent concept (UML generalization).",
            true, LinkLine.Solid, LinkArrow.Single, LinkColor.Secondary,
        ),
        LinkType(
            "specializes", LinkFamily.Hierarchy, "specializes", "%1\$s specializes %2\$s",
            "Car specializes Vehicle",
            "The source is the child concept.",
            true, LinkLine.Solid, LinkArrow.Single, LinkColor.Secondary,
        ),

        // ---- 2. Composition ----------------------------------------------
        LinkType(
            "part_of", LinkFamily.Composition, "part of", "%1\$s is part of %2\$s",
            "Wheel is part of Car",
            "The source is a component of the target.",
            true, LinkLine.Solid, LinkArrow.Single, LinkColor.Secondary,
        ),
        LinkType(
            "has_part", LinkFamily.Composition, "has part", "%1\$s has part %2\$s",
            "Car has part Wheel",
            "The target is a component of the source (inverse of part of).",
            true, LinkLine.Solid, LinkArrow.Single, LinkColor.Secondary,
        ),
        LinkType(
            "contains", LinkFamily.Composition, "contains", "%1\$s contains %2\$s",
            "Backlog contains Tasks",
            "Loose membership — the target sits inside the source.",
            true, LinkLine.Solid, LinkArrow.None, LinkColor.Secondary,
        ),
        LinkType(
            "composed_of", LinkFamily.Composition, "composed of", "%1\$s is composed of %2\$s",
            "Engine is composed of Parts",
            "Strong ownership — the parts exist only as part of the whole.",
            true, LinkLine.Double, LinkArrow.Single, LinkColor.Secondary,
        ),

        // ---- 3. Association ----------------------------------------------
        LinkType(
            "relates", LinkFamily.Association, "relates to", "%1\$s relates to %2\$s",
            "Sleep relates to Mood",
            "The neutral default: connected, with no claim about how.",
            false, LinkLine.Solid, LinkArrow.None, LinkColor.Outline,
        ),
        LinkType(
            "see_also", LinkFamily.Association, "see also", "%1\$s — see also %2\$s",
            "Pricing — see also Positioning",
            "A pointer for later reading, no semantic claim.",
            false, LinkLine.Dotted, LinkArrow.None, LinkColor.Outline,
        ),
        LinkType(
            "similar_to", LinkFamily.Association, "similar to", "%1\$s is similar to %2\$s",
            "Notion is similar to Coda",
            "Resemblance without being the same thing.",
            false, LinkLine.Dashed, LinkArrow.None, LinkColor.Outline,
        ),
        LinkType(
            "contrasts_with", LinkFamily.Association, "contrasts with", "%1\$s contrasts with %2\$s",
            "Speed contrasts with Quality",
            "A meaningful opposite or tension, weaker than contradicts.",
            false, LinkLine.Dashed, LinkArrow.None, LinkColor.Outline,
        ),
        LinkType(
            "inspired_by", LinkFamily.Association, "inspired by", "%1\$s is inspired by %2\$s",
            "This design is inspired by Brutalism",
            "Creative provenance — where the idea came from.",
            true, LinkLine.Dotted, LinkArrow.Single, LinkColor.Tertiary,
        ),

        // ---- 4. Equivalence ----------------------------------------------
        LinkType(
            "same_as", LinkFamily.Equivalence, "same as", "%1\$s is the same as %2\$s",
            "خواب is the same as Sleep",
            "One concept written two ways — interchangeable.",
            false, LinkLine.Double, LinkArrow.None, LinkColor.Outline,
        ),
        LinkType(
            "equivalent_to", LinkFamily.Equivalence, "equivalent to", "%1\$s is equivalent to %2\$s",
            "A and B are equivalent",
            "Logical equivalence: true together, false together.",
            false, LinkLine.Double, LinkArrow.Double, LinkColor.Primary,
        ),
        LinkType(
            "alias_of", LinkFamily.Equivalence, "alias of", "%1\$s is an alias of %2\$s",
            "MoM is an alias of Minutes of Meeting",
            "A naming variant of the same thing.",
            true, LinkLine.Double, LinkArrow.Single, LinkColor.Outline,
        ),
        LinkType(
            "close_match", LinkFamily.Equivalence, "close match", "%1\$s is a close match for %2\$s",
            "Process is a close match for Workflow",
            "Nearly the same — related, but not safe to swap.",
            false, LinkLine.Double, LinkArrow.None, LinkColor.Outline,
        ),

        // ---- 5. Causality -------------------------------------------------
        LinkType(
            "causes", LinkFamily.Causality, "causes", "%1\$s causes %2\$s",
            "Stress causes poor sleep",
            "Direct causal influence, one way.",
            true, LinkLine.Solid, LinkArrow.Single, LinkColor.Tertiary,
        ),
        LinkType(
            "leads_to", LinkFamily.Causality, "leads to", "%1\$s leads to %2\$s",
            "Practice leads to progress",
            "Softer than causes: a consequence over time.",
            true, LinkLine.Solid, LinkArrow.Single, LinkColor.Tertiary,
        ),
        LinkType(
            "enables", LinkFamily.Causality, "enables", "%1\$s enables %2\$s",
            "Sleep enables focus",
            "Necessary but not sufficient.",
            true, LinkLine.Dashed, LinkArrow.Single, LinkColor.Tertiary,
        ),
        LinkType(
            "prevents", LinkFamily.Causality, "prevents", "%1\$s prevents %2\$s",
            "Noise prevents concentration",
            "Blocks or reduces the target (negative influence).",
            true, LinkLine.Dashed, LinkArrow.Single, LinkColor.Error,
        ),
        LinkType(
            "increases", LinkFamily.Causality, "increases", "%1\$s increases %2\$s",
            "Training increases capacity",
            "Positive polarity: more of the source means more of the target.",
            true, LinkLine.Solid, LinkArrow.Single, LinkColor.Tertiary,
        ),
        LinkType(
            "decreases", LinkFamily.Causality, "decreases", "%1\$s decreases %2\$s",
            "Debt decreases freedom",
            "Negative polarity: more of the source means less of the target.",
            true, LinkLine.Solid, LinkArrow.Single, LinkColor.Error,
        ),
        LinkType(
            "influences", LinkFamily.Causality, "influences", "%1\$s influences %2\$s",
            "Weather influences mood",
            "A real effect whose direction you are not claiming yet.",
            true, LinkLine.Dotted, LinkArrow.Single, LinkColor.Tertiary,
        ),

        // ---- 6. Logic & argument ------------------------------------------
        LinkType(
            "implies", LinkFamily.Logic, "implies", "%1\$s implies %2\$s",
            "Rain implies wet ground",
            "Logical consequence: if the source then the target.",
            true, LinkLine.Solid, LinkArrow.Single, LinkColor.Primary,
        ),
        LinkType(
            "contradicts", LinkFamily.Logic, "contradicts", "%1\$s contradicts %2\$s",
            "Flat earth contradicts Gravity",
            "The two cannot both be true.",
            false, LinkLine.Dashed, LinkArrow.None, LinkColor.Error,
        ),
        LinkType(
            "supports", LinkFamily.Logic, "supports", "%1\$s supports %2\$s",
            "Benchmarks support this claim",
            "Evidence in favour of the target (IBIS argument).",
            true, LinkLine.Dotted, LinkArrow.Single, LinkColor.Primary,
        ),
        LinkType(
            "objects_to", LinkFamily.Logic, "objects to", "%1\$s objects to %2\$s",
            "Cost objects to this plan",
            "A stated objection to the target (IBIS argument).",
            true, LinkLine.Dashed, LinkArrow.Single, LinkColor.Error,
        ),
        LinkType(
            "refutes", LinkFamily.Logic, "refutes", "%1\$s refutes %2\$s",
            "The data refutes the assumption",
            "Stronger than contradicts: it disproves the target.",
            true, LinkLine.Solid, LinkArrow.Single, LinkColor.Error,
        ),
        LinkType(
            "answers", LinkFamily.Logic, "answers", "%1\$s answers %2\$s",
            "Option A answers Which database?",
            "A position answering a question (IBIS issue → position).",
            true, LinkLine.Solid, LinkArrow.Single, LinkColor.Primary,
        ),
        LinkType(
            "questions", LinkFamily.Logic, "questions", "%1\$s questions %2\$s",
            "Churn questions the pricing plan",
            "Raises a doubt about the target.",
            true, LinkLine.Dotted, LinkArrow.Single, LinkColor.Primary,
        ),
        LinkType(
            "assumes", LinkFamily.Logic, "assumes", "%1\$s assumes %2\$s",
            "This plan assumes cheap power",
            "An unstated premise the source depends on.",
            true, LinkLine.Dotted, LinkArrow.Single, LinkColor.Outline,
        ),
        LinkType(
            "because", LinkFamily.Logic, "because", "%1\$s because %2\$s",
            "Growth slowed because demand fell",
            "Gives the reason — the target explains the source.",
            true, LinkLine.Solid, LinkArrow.Single, LinkColor.Secondary,
        ),
        LinkType(
            "therefore", LinkFamily.Logic, "therefore", "%1\$s therefore %2\$s",
            "Demand fell therefore revenue dropped",
            "States the conclusion that follows.",
            true, LinkLine.Solid, LinkArrow.Single, LinkColor.Secondary,
        ),

        // ---- 7. Process & time --------------------------------------------
        LinkType(
            "depends_on", LinkFamily.Process, "depends on", "%1\$s depends on %2\$s",
            "Launch depends on testing",
            "A prerequisite: the source needs the target first.",
            true, LinkLine.Dashed, LinkArrow.Single, LinkColor.Tertiary,
        ),
        LinkType(
            "requires", LinkFamily.Process, "requires", "%1\$s requires %2\$s",
            "Build requires an API key",
            "A hard requirement.",
            true, LinkLine.Solid, LinkArrow.Single, LinkColor.Tertiary,
        ),
        LinkType(
            "blocks", LinkFamily.Process, "blocks", "%1\$s blocks %2\$s",
            "Bug blocks the release",
            "Stops the target from proceeding.",
            true, LinkLine.Solid, LinkArrow.Single, LinkColor.Error,
        ),
        LinkType(
            "before", LinkFamily.Process, "before", "%1\$s comes before %2\$s",
            "Draft comes before Review",
            "Ordering in time.",
            true, LinkLine.Dashed, LinkArrow.Single, LinkColor.Outline,
        ),
        LinkType(
            "after", LinkFamily.Process, "after", "%1\$s comes after %2\$s",
            "Review comes after Draft",
            "Ordering in time (inverse of before).",
            true, LinkLine.Dashed, LinkArrow.Single, LinkColor.Outline,
        ),
        LinkType(
            "replaces", LinkFamily.Process, "replaces", "%1\$s replaces %2\$s",
            "Plan B replaces Plan A",
            "Supersedes an earlier version.",
            true, LinkLine.Solid, LinkArrow.Single, LinkColor.Outline,
        ),
    )

    private val byKey: Map<String, LinkType> = all.associateBy { it.key }

    val default: LinkType get() = byKey.getValue(DEFAULT_KEY)

    fun byKeyOrNull(key: String): LinkType? = byKey[key]

    /** Falls back to the default type, so unknown keys from an older file never crash. */
    fun byKey(key: String): LinkType = byKey[key] ?: default

    /** The catalog grouped by family, in declaration order — used by the picker and Help. */
    fun byFamily(): List<Pair<LinkFamily, List<LinkType>>> =
        LinkFamily.values().map { family -> family to all.filter { it.family == family } }

    /**
     * Reads a link as a sentence. A custom label overrides the built-in phrase, which is the
     * escape hatch for relations the catalog does not name.
     */
    fun sentence(type: LinkType, from: String, to: String, label: String? = null): String =
        if (!label.isNullOrBlank()) {
            "$from $label $to"
        } else {
            type.readAs.replace("%1\$s", from).replace("%2\$s", to)
        }

    /** The text drawn on the board and in the list: the type name, or the custom label. */
    fun displayLabel(type: LinkType, label: String?): String =
        if (!label.isNullOrBlank()) label else type.name
}