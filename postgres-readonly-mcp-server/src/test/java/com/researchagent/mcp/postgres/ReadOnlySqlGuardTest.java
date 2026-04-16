package com.researchagent.mcp.postgres;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReadOnlySqlGuardTest {

    private final ReadOnlySqlGuard guard = new ReadOnlySqlGuard();

    @Test
    void allowsSelectQueries() {
        assertDoesNotThrow(() -> guard.validate("select * from users"));
        assertDoesNotThrow(() -> guard.validate("with recent as (select * from users) select * from recent"));
        assertDoesNotThrow(() -> guard.validate("show search_path"));
    }

    @Test
    void rejectsWriteStatements() {
        assertThrows(IllegalArgumentException.class, () -> guard.validate("update users set name = 'x'"));
        assertThrows(IllegalArgumentException.class, () -> guard.validate("with doomed as (delete from users returning *) select * from doomed"));
    }

    @Test
    void rejectsDangerousHelpersAndMultipleStatements() {
        assertThrows(IllegalArgumentException.class, () -> guard.validate("select pg_read_file('/etc/passwd')"));
        assertThrows(IllegalArgumentException.class, () -> guard.validate("select 1; select 2"));
    }
}
