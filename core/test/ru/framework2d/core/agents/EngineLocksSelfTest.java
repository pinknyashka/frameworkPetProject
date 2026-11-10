package ru.framework2d.core.agents;

/**
 * Runnable lock-layer verification. No JUnit dependency; run it with
 * {@code .\gradlew.bat :core:lockSelfTest}.
 *
 * <p>Scenarios: shared readers, writer rejection diagnostics, read reentrancy,
 * upgrade rejection, canonical lock order, writer wait/release, timeout
 * diagnostics, and cross-agent test-scope isolation.</p>
 */
public final class EngineLocksSelfTest {

    private static int failures;

    private EngineLocksSelfTest() {
    }

    public static void main(String[] args) throws Exception {
        sharedReaders();
        readReentrancy();
        upgradeRejected();
        lockOrderEnforced();
        writerWaitsForRelease();
        timeoutCarriesStatus();
        testScopeIsolates();

        System.out.println();
        if (failures == 0) {
            System.out.println("ALL LOCK TESTS PASSED");
        } else {
            System.out.println(failures + " LOCK TEST(S) FAILED");
            System.exit(1);
        }
    }

    private static void sharedReaders() {
        ResourceLease readerA = EngineLocks.acquire(EngineResource.GRAPHICS,
                LockMode.READ, "agent-A", "inspect", 0L);
        ResourceLease readerB = EngineLocks.acquire(EngineResource.GRAPHICS,
                LockMode.READ, "agent-B", "inspect", 0L);

        LockStatus shared = EngineLocks.status(EngineResource.GRAPHICS);
        check(shared.holders().size() == 2, "two readers share GRAPHICS");
        check(shared.allowsRead() && !shared.allowsWrite(),
                "shared status allows read, not write");

        try {
            EngineLocks.acquire(EngineResource.GRAPHICS, LockMode.WRITE,
                    "agent-C", "mutate", 0L);
            check(false, "writer rejected while readers hold");
        } catch (LockBusyException e) {
            check(e.status().holders().size() == 2, "busy status lists both readers");
            check(e.status().allowsRead(), "busy status still permits reads");
            System.out.println("  " + e.describe());
        } finally {
            readerA.close();
            readerB.close();
        }
    }

    private static void readReentrancy() {
        ResourceLease outer = EngineLocks.acquire(EngineResource.GRAPHICS,
                LockMode.READ, "agent-A", "outer", 0L);
        ResourceLease inner = EngineLocks.acquire(EngineResource.GRAPHICS,
                LockMode.READ, "agent-A", "inner", 0L);

        LockStatus status = EngineLocks.status(EngineResource.GRAPHICS);
        check(status.holders().size() == 1 && status.holders().get(0).reentrant() == 2,
                "same owner read lease is reentrant");

        inner.close();
        outer.close();
        check(EngineLocks.status(EngineResource.GRAPHICS).isFree(),
                "GRAPHICS free after release");
    }

    private static void upgradeRejected() {
        ResourceLease hold = EngineLocks.acquire(EngineResource.MODEL,
                LockMode.READ, "agent-A", "read model", 0L);
        try {
            EngineLocks.acquire(EngineResource.MODEL, LockMode.WRITE,
                    "agent-A", "upgrade", 0L);
            check(false, "upgrade rejected");
        } catch (IllegalStateException e) {
            check(e.getMessage().contains("upgrade"), "upgrade error names the problem");
        } finally {
            hold.close();
        }
    }

    private static void lockOrderEnforced() {
        ResourceLease hold = EngineLocks.acquire(EngineResource.GRAPHICS,
                LockMode.READ, "agent-order", "graphics", 0L);
        try {
            EngineLocks.acquire(EngineResource.MODEL, LockMode.WRITE,
                    "agent-order", "model", 0L);
            check(false, "lock order violation detected");
        } catch (LockOrderException e) {
            check(e.requested() == EngineResource.MODEL
                            && e.held() == EngineResource.GRAPHICS,
                    "LockOrderException reports requested/held domains");
        } finally {
            hold.close();
        }
    }

    private static void writerWaitsForRelease() throws InterruptedException {
        Thread holder = new Thread(new Runnable() {
            public void run() {
                try (ResourceLease lease = EngineLocks.acquire(EngineResource.SOUND,
                        LockMode.WRITE, "holder", "hold sound", 2000L)) {
                    Thread.sleep(150L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "holder-thread");
        holder.start();

        Thread.sleep(50L);
        long start = System.currentTimeMillis();
        ResourceLease waiter = EngineLocks.acquire(EngineResource.SOUND,
                LockMode.WRITE, "waiter", "wait for sound", 2000L);
        long waited = System.currentTimeMillis() - start;
        check(waited >= 50L, "writer waited for the holder (" + waited + " ms)");
        waiter.close();
        holder.join();
    }

    private static void timeoutCarriesStatus() {
        ResourceLease hold = EngineLocks.acquire(EngineResource.SOUND,
                LockMode.WRITE, "holder2", "long hold", 0L);
        try {
            EngineLocks.acquire(EngineResource.SOUND, LockMode.READ,
                    "waiter2", "timeout probe", 100L);
            check(false, "timeout raised");
        } catch (LockTimeoutException e) {
            check(e.status().heldBy("holder2"), "timeout status names the holder");
            check(e.status().holders().get(0).reason().equals("long hold"),
                    "timeout status carries the reason");
            check(e.waitedMillis() >= 80L, "timeout reports waited time");
            System.out.println("  " + e.describe());
        } finally {
            hold.close();
        }
    }

    private static void testScopeIsolates() throws InterruptedException {
        EngineLocks.setAgentId("test-agent");
        AgentTestScope scope = EngineLocks.beginTestScope(
                "pause graphics and sound",
                EngineResource.GRAPHICS, EngineResource.SOUND);
        check(scope.resources().size() == 2, "test scope acquired two domains");

        final boolean[] probeResult = new boolean[1];
        Thread probe = new Thread(new Runnable() {
            public void run() {
                EngineLocks.setAgentId("probe-agent");
                probeResult[0] = EngineLocks.isAvailable(
                        EngineResource.GRAPHICS, LockMode.READ);
            }
        }, "probe-thread");
        probe.start();
        probe.join();
        check(!probeResult[0], "test scope blocks another agent's read");

        scope.close();
        EngineLocks.setAgentId("after-agent");
        check(EngineLocks.isAvailable(EngineResource.GRAPHICS, LockMode.READ),
                "domains readable after test scope closes");
        EngineLocks.clearAgentId();
    }

    private static void check(boolean condition, String message) {
        if (condition) {
            System.out.println("PASS: " + message);
        } else {
            failures++;
            System.out.println("FAIL: " + message);
        }
    }
}
