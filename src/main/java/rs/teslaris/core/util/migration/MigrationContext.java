package rs.teslaris.core.util.migration;

/**
 * Marks the current thread as performing a migration from a configured source.
 * <p>
 * Migrated data arrives in bulk and is frequently dirty, so the per-record bookkeeping that serves
 * interactive editing is pure overhead here: identifier patterns are not worth enforcing against
 * records that are known to be malformed, and recording a revision plus a quality assessment plus
 * an assessment classification for every imported entity costs far more than it informs. Those
 * steps are skipped while a context is open and are meant to be performed in bulk afterwards,
 * {@code RevisionService.backfillRevision} creates the missing baseline revisions, and the
 * classification scheduled tasks reclassify what was imported.
 * <p>
 * Caveat: the context is thread-bound. It covers the synchronous call tree of a migration run,
 * which is how {@code MigrationPipelineRunner} executes, but it does not propagate into parallel
 * streams or asynchronous work. Any pipeline that dispatches item creation onto an executor has to
 * open a context of its own on each worker.
 */
public final class MigrationContext {

    private static final ThreadLocal<Boolean> ACTIVE = new ThreadLocal<>();


    private MigrationContext() {
    }

    public static void runDuring(Runnable migration) {
        if (isActive()) {
            throw new IllegalStateException("Migration context is already open on this thread.");
        }

        ACTIVE.set(Boolean.TRUE);

        try {
            migration.run();
        } finally {
            ACTIVE.remove();
        }
    }

    public static boolean isActive() {
        return Boolean.TRUE.equals(ACTIVE.get());
    }
}
