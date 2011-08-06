package org.basex.test.performance;

import org.basex.BaseXServer;
import org.basex.core.Text;
import org.basex.core.cmd.CreateDB;
import org.basex.core.cmd.DropDB;
import org.basex.core.cmd.XQuery;
import org.basex.server.ClientSession;
import org.basex.util.Performance;
import org.basex.util.Util;
import org.junit.Test;

/**
 * Testing concurrent XQUF statements on a single database.
 *
 * @author BaseX Team 2005-11, BSD License
 * @author Lukas Kircher
 */
public final class XQUFServerStressTest {
  /** Database name. */
  private static final String DBNAME = Util.name(XQUFServerStressTest.class);
  /** Server. */
  private static BaseXServer server;

  /**
   * Runs the test.
   * @throws Exception exception
   */
  @Test
  public void clients10runs10() throws Exception {
    run(10, 10);
  }

  /**
   * Runs the test.
   * @throws Exception exception
   */
  @Test
  public void clients10runs50() throws Exception {
    run(10, 50);
  }

  /**
   * Runs the test.
   * @throws Exception exception
   */
  @Test
  public void clients50runs10() throws Exception {
    run(50, 10);
  }

  /**
   * Runs the test.
   * @throws Exception exception
   */
  @Test
  public void clients50runs50() throws Exception {
    run(50, 50);
  }

  /**
   * Runs the stress test.
   * @param clients number of clients
   * @param runs number of runs per client
   * @throws Exception exception
   */
  private void run(final int clients, final int runs) throws Exception {
    // Run server instance
    server = new BaseXServer("-z");
    insert(clients, runs);
    delete(clients, runs);
    final ClientSession s = newSession();
    s.execute(new DropDB(DBNAME));
    s.close();
    server.stop();
  }

  /**
   * Performs the query.
   * @param clients number of clients
   * @param runs number of runs
   * @throws Exception exception
   */
  private void insert(final int clients, final int runs) throws Exception {
    final ClientSession s = newSession();
    s.execute(new CreateDB(DBNAME, "<doc/>"));
    s.close();
    run("insert node <node/> into doc('" + DBNAME + "')/doc", clients, runs);
  }

  /**
   * Performs the query.
   * @param clients number of clients
   * @param runs number of runs
   * @throws Exception exception
   */
  private void delete(final int clients, final int runs) throws Exception {
    final ClientSession s = newSession();
    s.execute(new CreateDB(DBNAME, "<doc/>"));
    final int c = 100 + clients * clients;
    s.execute(new XQuery("for $i in 1 to " + c +
        " return insert node <node/> into doc('" + DBNAME + "')/doc"));
    s.close();
    run("delete nodes (doc('" + DBNAME + "')/doc/node)[1]", clients, runs);
  }

  /**
   * Starts concurrent client operations.
   * @param query test query
   * @param clt number of clients
   * @param runs number of runs
   * @throws Exception exception
   */
  private void run(final String query, final int clt, final int runs)
      throws Exception {

    final Client[] cl = new Client[clt];
    for(int i = 0; i < clt; ++i) cl[i] = new Client(query, runs);
    for(final Client c : cl) c.start();
    for(final Client c : cl) c.join();
  }

  /**
   * Returns a session instance.
   * @return session
   * @throws Exception exception
   */
  static ClientSession newSession() throws Exception {
    return new ClientSession(server.context, Text.ADMIN, Text.ADMIN);
  }

  /** Single client. */
  static final class Client extends Thread {
    /** Client session. */
    private ClientSession session;
    /** Query to be executed by this client. */
    final String query;
    /** Number of runs. */
    private final int runs;

    /**
     * Default constructor.
     * @param qu query string
     * @param r number of runs
     * @throws Exception exception
     */
    Client(final String qu, final int r) throws Exception {
      session = newSession();
      query = qu;
      runs = r;
    }

    @Override
    public void run() {
      try {
        for(int i = 0; i < runs; ++i) {
          Performance.sleep(100);
          session.execute("xquery " + query);
        }
        session.close();
      } catch(final Exception ex) {
        ex.printStackTrace();
      }
    }
  }
 }