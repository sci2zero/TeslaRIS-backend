package rs.teslaris.core.assessment.ruleengine;

public abstract class JournalClassificationRuleEngine {

    public void startClassification() {
        // TODO: Load journals in batches, call all classification rules,
        //  implement mechanism for entity indicator caching
    }

    abstract public boolean handleM21APlus(Integer journalIndex);

    abstract public boolean handleM21A(Integer journalIndex);

    abstract public boolean handleM21(Integer journalIndex);

    abstract public boolean handleM22(Integer journalIndex);

    abstract public boolean handleM23(Integer journalIndex);

    abstract public boolean handleM23E(Integer journalIndex);

    abstract public boolean handleM24Plus(Integer journalIndex);

    abstract public boolean handleM24(Integer journalIndex);

    abstract public boolean handleM51(Integer journalIndex);

    abstract public boolean handleM52(Integer journalIndex);

    abstract public boolean handleM53(Integer journalIndex);

    abstract public boolean handleM54(Integer journalIndex);
}
