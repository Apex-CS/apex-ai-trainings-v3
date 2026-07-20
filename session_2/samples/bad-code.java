public class UserDao {
    static Connection conn;
    public List<String> getUsers(String name) throws Exception {
        conn = DriverManager.getConnection("jdbc:mysql://prod-db/users", "root", "root123");
        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery("SELECT * FROM users WHERE name = '" + name + "'");
        List<String> l = new ArrayList();
        while (rs.next()) l.add(rs.getString(1));
        return l;
    }
}
