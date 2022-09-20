package cat.nyaa.nyaacoretester.orm;

import cat.nyaa.nyaacore.orm.DatabaseUtils;
import cat.nyaa.nyaacore.orm.NonUniqueResultException;
import cat.nyaa.nyaacore.orm.RollbackGuard;
import cat.nyaa.nyaacore.orm.WhereClause;
import cat.nyaa.nyaacore.orm.backends.BackendConfig;
import cat.nyaa.nyaacore.orm.backends.IConnectedDatabase;
import cat.nyaa.nyaacore.orm.backends.ITypedTable;
import cat.nyaa.nyaacoretester.NyaaCoreTester;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class SQLiteDatabaseTest {

    private IConnectedDatabase db;

    @BeforeAll
    public static void touchDbFile() throws SQLException, ClassNotFoundException {
        new File(NyaaCoreTester.instance.getDataFolder(), "testdb.db").delete();
        DatabaseUtils.connect(NyaaCoreTester.instance, BackendConfig.sqliteBackend("testdb.db")).close();
    }

    @BeforeEach
    public void openDatabase() throws SQLException, ClassNotFoundException {
        db = DatabaseUtils.connect(NyaaCoreTester.instance, BackendConfig.sqliteBackend("testdb.db"));
    }

    @Test
    public void testClear() {
        ITypedTable<TableTest1> tableTest1 = db.getTable(TableTest1.class);
        tableTest1.delete(WhereClause.EMPTY);
        assertEquals(0, tableTest1.count(WhereClause.EMPTY));
    }

    @Test
    public void testInsert() {
        ITypedTable<TableTest1> tableTest1 = db.getTable(TableTest1.class);
        tableTest1.delete(WhereClause.EMPTY);
        tableTest1.insert(new TableTest1(1L, "test", UUID.randomUUID(), UUID.randomUUID()));
        tableTest1.insert(new TableTest1(2L, "test", UUID.randomUUID(), UUID.randomUUID()));
        assertEquals(2, tableTest1.count(WhereClause.EMPTY));
    }

    @Test
    public void testSelect() {
        ITypedTable<TableTest1> tableTest1 = db.getTable(TableTest1.class);
        tableTest1.delete(WhereClause.EMPTY);
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        TableTest1 record = new TableTest1(42L, "test", uuid1, uuid2);
        tableTest1.insert(record);

        List<TableTest1> ret = tableTest1.select(WhereClause.EMPTY);
        assertEquals(1, ret.size());
        assertEquals(record, ret.get(0));
    }

    @Test
    public void testDelete() throws NonUniqueResultException {
        ITypedTable<TableTest1> tableTest1 = db.getTable(TableTest1.class);
        tableTest1.delete(WhereClause.EMPTY);
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        TableTest1 record = new TableTest1(42L, "test", uuid1, uuid2);
        tableTest1.insert(record);
        tableTest1.insert(new TableTest1(2L, "test", UUID.randomUUID(), UUID.randomUUID()));
        assertEquals(2, tableTest1.count(WhereClause.EMPTY));
        tableTest1.delete(WhereClause.EQ("id", 2));
        assertEquals(record, tableTest1.selectUnique(WhereClause.EMPTY));
    }

    @Test
    public void testUpdate() throws NonUniqueResultException {
        ITypedTable<TableTest1> tableTest1 = db.getTable(TableTest1.class);
        tableTest1.delete(WhereClause.EMPTY);
        TableTest1 record1 = new TableTest1(42L, "test", UUID.randomUUID(), UUID.randomUUID());
        TableTest1 record2 = new TableTest1(43L, "test2", UUID.randomUUID(), UUID.randomUUID());
        TableTest1 record1_5 = new TableTest1(42L, "test", record2.uuid, record2.uuid_indirect);

        tableTest1.insert(record1);
        assertEquals(record1, tableTest1.selectUnique(WhereClause.EMPTY));
        tableTest1.update(record2, WhereClause.EQ("id", 42), "uuid", "uuid_indirect");
        assertEquals(record1_5, tableTest1.selectUnique(WhereClause.EMPTY));
        tableTest1.update(record2, WhereClause.EQ("id", 42));
        assertEquals(record2, tableTest1.selectUnique(WhereClause.EMPTY));
    }

    @Test
    public void testWhereClause() {
        ITypedTable<TableTest1> tableTest1 = db.getTable(TableTest1.class);
        tableTest1.delete(WhereClause.EMPTY);
        for (long i = 1; i <= 10; i++) {
            tableTest1.insert(new TableTest1(i, "test", UUID.randomUUID(), UUID.randomUUID()));
        }

        for (long i = 1; i <= 10; i++) {
            assertEquals(i, tableTest1.count(new WhereClause("id", "<=", i)));
        }
    }

    @Test
    public void testWhereClause2() throws Exception {
        ITypedTable<TableTest1> tableTest1 = db.getTable(TableTest1.class);
        tableTest1.delete(WhereClause.EMPTY);
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        TableTest1 record = new TableTest1(42L, "test", uuid1, uuid2);
        tableTest1.insert(record);

        assertEquals(record, tableTest1.selectUnique(WhereClause.EQ("id", 42)));
        assertEquals(record, tableTest1.selectUnique(WhereClause.EQ("id", 42L)));
        assertEquals(record, tableTest1.selectUnique(WhereClause.EQ("string", "test")));
        assertEquals(record, tableTest1.selectUnique(WhereClause.EQ("uuid", uuid1)));
        assertEquals(record, tableTest1.selectUnique(WhereClause.EQ("uuid_indirect", uuid2)));
    }

    @Test
    public void testItemSerialization() throws NonUniqueResultException {
        ITypedTable<TableTest2> tb2 = db.getTable(TableTest2.class);
        tb2.delete(WhereClause.EMPTY);

        TableTest2 r = new TableTest2();
        r.id = 42;
        r.item = new ItemStack(Material.OAK_WOOD);
        tb2.insert(r);
        ItemStack ret = tb2.selectUnique(WhereClause.EQ("id", 42)).item;
        assertEquals(r.item, ret);
    }

    @Test
    public void testQueryBundled() {
        ITypedTable<TableTest3> tb3 = db.getTable(TableTest3.class);
        ITypedTable<TableTest4> tb4 = db.getTable(TableTest4.class);

        tb3.delete(WhereClause.EMPTY);
        tb4.delete(WhereClause.EMPTY);

        for (long i = 0; i < 100; i++) {
            db.queryBundledAs(NyaaCoreTester.instance, "table3_4_insert.sql", Collections.singletonMap("table_name", "test3"), null, i, i * 2, i * 3);
            db.queryBundledAs(NyaaCoreTester.instance, "table3_4_insert.sql", Collections.singletonMap("table_name", "test4"), null, i, i * 4, i * 5);
        }
        db.queryBundledAs(NyaaCoreTester.instance, "table3_update.sql", Collections.emptyMap(), null);

        List<TableTest3> list = tb3.select(WhereClause.EMPTY);
        for (TableTest3 rr : list) {
            assertEquals(rr.key * 3, rr.data2);
            if (rr.key % 4 == 0) {
                assertEquals(rr.key * 3 / 4, rr.data1);
            } else {
                assertEquals(rr.key * 2, rr.data1);
            }
        }
    }

    @Test
    public void testQueryBundledAs() {
        ITypedTable<TableTest5> tb5 = db.getTable(TableTest5.class);
        tb5.delete(WhereClause.EMPTY);
        tb5.insert(new TableTest5("alice", 10, "player"));
        tb5.insert(new TableTest5("bob", 33, "admin"));
        tb5.insert(new TableTest5("cat", 8, "dev"));
        tb5.insert(new TableTest5("dave", 16, "dev"));
        tb5.insert(new TableTest5("eva", 22, "player"));
        tb5.insert(new TableTest5("fang", 21, "creator"));

        List<TableTest5.CollectedReport> result = db.queryBundledAs(NyaaCoreTester.instance, "table5_query.sql", Collections.emptyMap(), TableTest5.CollectedReport.class);
        assertEquals(4, result.size());
        assertEquals(new TableTest5.CollectedReport("admin", 33, 1), result.get(0));
        assertEquals(new TableTest5.CollectedReport("creator", 21, 1), result.get(1));
        assertEquals(new TableTest5.CollectedReport("dev", 16, 2), result.get(2));
        assertEquals(new TableTest5.CollectedReport("player", 22, 2), result.get(3));
    }

    @Test
    public void testNullColumns() throws NonUniqueResultException {
        ITypedTable<TableTest6> tb6 = db.getTable(TableTest6.class);

        tb6.delete(WhereClause.EMPTY);
        tb6.insert(new TableTest6("key1", "val1"));
        tb6.insert(new TableTest6("key2", null));
        assertEquals("val1", tb6.selectUnique(WhereClause.EQ("key", "key1")).value);
        assertEquals(null, tb6.selectUnique(WhereClause.EQ("key", "key2")).value);
        assertEquals("key1", tb6.selectUnique(WhereClause.EQ("value", "val1")).key);
        assertEquals("key2", tb6.selectUnique(new WhereClause("value", " IS ", null)).key);
    }

    @Test
    public void testUniqueColumns() throws NonUniqueResultException {
        ITypedTable<TableTest7> tb7 = db.getTable(TableTest7.class);

        tb7.delete(WhereClause.EMPTY);
        tb7.insert(new TableTest7(1, 2L, 3L));
        tb7.insert(new TableTest7(2, 2L, 4L));

        boolean exceptionCaught = false;
        try {
            tb7.insert(new TableTest7(3, 2L, 4L));
        } catch (RuntimeException ex) {
            exceptionCaught = true;
            SQLException sqlexp = (SQLException) ex.getCause();
            assertTrue("UNIQUE ERROR", sqlexp.getMessage().contains("UNIQUE"));
            assertTrue("UNIQUE COLUMN NAME", sqlexp.getMessage().contains("test7.val_uniq"));
        }

        assertTrue("Exception thrown", exceptionCaught);
    }

    @Test
    public void testAllTypes() throws NonUniqueResultException {
        ITypedTable<TableAllTypes> tb = db.getTable(TableAllTypes.class);

        TableAllTypes rec = TableAllTypes.instance();
        tb.delete(WhereClause.EMPTY);
        tb.insert(rec);
        assertEquals(rec, tb.selectUnique(WhereClause.EMPTY));
    }

    @Test
    public void testSchemaVerification() {
        Class[] tables = {
                TableTest1.class,
                TableTest2.class,
                TableTest3.class,
                TableTest4.class,
                TableTest5.class,
                TableTest6.class,
                TableTest7.class,
                TableTest8.class,
                TableAllTypes.class
        };
        for (Class c : tables) {
            assertNotNull(db.getTable(c));
            assertNotNull(db.getTable(c));
        }
    }

    @Test
    public void testAutoIncr() {
        // mark an `Integer' field as `primary', then set it to null on insertion
        ITypedTable<TableTest8> tb8 = db.getTable(TableTest8.class);

        tb8.delete(WhereClause.EMPTY);
        tb8.insert(new TableTest8(null, "A"));
        tb8.insert(new TableTest8(null, "B"));
        tb8.insert(new TableTest8(null, "C"));

        List<TableTest8> r = tb8.select(WhereClause.EMPTY);
        assertEquals(3, r.size());
        assertEquals(new TableTest8(1, "A"), r.get(0));
        assertEquals(new TableTest8(2, "B"), r.get(1));
        assertEquals(new TableTest8(3, "C"), r.get(2));
    }

    @Test
    public void testRollbackGuard() throws Exception {
        ITypedTable<TableTest1> tableTest1 = db.getTable(TableTest1.class);
        tableTest1.delete(WhereClause.EMPTY);
        TableTest1 record = new TableTest1(1L, "test", UUID.randomUUID(), UUID.randomUUID());
        tableTest1.insert(record);

        try (RollbackGuard guard1 = new RollbackGuard(db)) {
            TableTest1 r1 = tableTest1.selectUnique(WhereClause.EMPTY);
            r1.string += "_suffix_transaction";
            tableTest1.update(r1, WhereClause.EMPTY, "string");
            guard1.commit();
        }

        assertEquals("test_suffix_transaction", tableTest1.selectUnique(WhereClause.EMPTY).string);
    }

    @Test
    public void testRollbackGuard2() throws Exception {
        ITypedTable<TableTest1> tableTest1 = db.getTable(TableTest1.class);
        tableTest1.delete(WhereClause.EMPTY);
        TableTest1 record = new TableTest1(1L, "test", UUID.randomUUID(), UUID.randomUUID());
        tableTest1.insert(record);

        try (RollbackGuard guard1 = new RollbackGuard(db)) {
            TableTest1 r1 = tableTest1.selectUnique(WhereClause.EMPTY);
            r1.string += "_suffix_transaction";
            tableTest1.update(r1, WhereClause.EMPTY, "string");
        }

        assertEquals("test", tableTest1.selectUnique(WhereClause.EMPTY).string);
    }

    private static Integer test_councurrency_try_count = 0;

    /**
     * You can do concurrency+transaction, but that's very discouraged.
     * You have to deal with locking and error recovery on your own.
     */
    @Test
    public void testConcurrency() throws Exception {
        List<Thread> threads = new ArrayList<>(200);
        ITypedTable<TableTest8> tb8 = db.getTable(TableTest8.class);
        tb8.delete(WhereClause.EMPTY);
        tb8.insert(new TableTest8(0, ""));
        test_councurrency_try_count = 0;

        for (int thread_no = 1; thread_no <= 100; thread_no++) {
            final int tn = thread_no;
            Thread t = new Thread(() -> {
                try {
                    IConnectedDatabase db = DatabaseUtils.connect(NyaaCoreTester.instance, BackendConfig.sqliteBackend("testdb.db"));
                    try {
                        ITypedTable<TableTest8> tb = db.getTable(TableTest8.class);
                        do {
                            synchronized (SQLiteDatabaseTest.this) {
                                test_councurrency_try_count++;
                            }

                            try (RollbackGuard guard = new RollbackGuard(db)) { // guard begins a transaction
                                TableTest8 record = tb.selectUnique(WhereClause.EMPTY);
                                record.x += tn;  // nth thread inc the value by n
                                tb.update(record, WhereClause.EMPTY);
                                guard.commit();
                                return; // return if update success
                            } catch (Exception ex) {

                            }
                            Thread.sleep(tn); // don't retry that fast.
                        } while (true); // retry if transaction failed, FIXME check if it's actually a transaction failure
                    } finally {  //never forget to close db connection
                        db.close();
                    }
                } catch (Exception ex) {  // "catch 'em all" try-block
                    throw new RuntimeException(ex);
                }
            });
            threads.add(t);
            t.start();
        }

        for (Thread t : threads) t.join();
        NyaaCoreTester.instance.getLogger().info(String.format("testConcurrency: try count: %d", test_councurrency_try_count));
        assertEquals(5050, (Object) tb8.selectUnique(WhereClause.EMPTY).x);  // Sigma{i=1..100}{i} == 5050
    }

    @AfterEach
    public void closeTables() throws SQLException {
        db.close();
    }
}
