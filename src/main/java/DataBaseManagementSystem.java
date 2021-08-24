import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.SecureRandom;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DataBaseManagementSystem {

    private static DataBaseManagementSystem DBMS = null;
    private Connection connection = null;
    private BufferedReader reader = null;
    private Statement statement = null;
    static final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    private DataBaseManagementSystem() {
        try {
            reader = new BufferedReader(new InputStreamReader(System.in));
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(
                    "jdbc:sqlite:well.db"
            );
            System.out.println("Соединение с базой данных установленно.");
            statement = connection.createStatement();
            createTableIfNotExist();
        } catch (SQLException | ClassNotFoundException e) {
            System.out.println(e.getMessage());
        }
    }

    private void createTableIfNotExist() {
        String creatingTableWells = "create table if not exists wells(" +
                "id integer primary key autoincrement not null," +
                "name varchar(32) not null," +
                "numberEquipment integer not null," +
                "unique(id, name)" +
                ");";
        try {
            statement.executeUpdate(creatingTableWells);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static DataBaseManagementSystem getDBMS() {
        if (DBMS == null)
            return new DataBaseManagementSystem();
        else
            return DBMS;
    }

    void insert() {
        try {
            System.out.println("Введите название скважины : ");
            String name = reader.readLine().toLowerCase(Locale.ROOT);
            System.out.println("Введите количество оборудования : ");
            int numberEquipment = Integer.parseInt(reader.readLine());
            if(numberEquipment<1) {
                System.out.println("Вы ввели 0 или отрицательное количество оборудования.");
                insert();
            }
            if(isExist(name)) {
                System.out.println("Такая скважина числиться в БД.");
                System.out.println("Введите 1 что бы добавить количество оборудования к существующей скважине.");
                System.out.println("Нажмите 2 для повтора ввода.");
                String command = reader.readLine();
                if(command.equals("1")) {
                    addToExistingWell(name, numberEquipment);
                } else if(command.equals("2")) {
                    insert();
                }
            } else {

                // Добавление строк в таблицу скважины.
                String insertIntoWellsNewWellWithEquipment =
                        "INSERT INTO wells (name, numberEquipment) " +
                                "VALUES ( '" + name + "', '" + numberEquipment + "')";
                statement.executeUpdate(insertIntoWellsNewWellWithEquipment);

                // Создание таблицы оборудование если она не существует.
                String createTableEquipmentIfNotExist = "create table if not exists " + name + "(" +
                        "id integer primary key autoincrement not null," +
                        "name varchar(10) not null," +
                        "unique(id, name)" +
                        ");";
                statement.executeUpdate(createTableEquipmentIfNotExist);


                // Добавление строк в таблицу оборудование.
                insertIntoEquipmentTable(numberEquipment, name);

                System.out.println("Rows added.");

            }
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
    }

    private void insertIntoEquipmentTable(int numberEquipment, String name) {
        ArrayList<String> uniqueNames = getArrayListUniqueNames(numberEquipment);
        for (String uniqueName  : uniqueNames) {
            String insertIntoEquipment = "" +
                    "INSERT INTO " + name + "(name) " +
                    "values('" + uniqueName +  "'" +
                    ");";
            try {
                statement.executeUpdate(insertIntoEquipment);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

    }

    private ArrayList<String> getArrayListUniqueNames(int number) {
        ArrayList<String> result = new ArrayList<>();
        SecureRandom rnd = new SecureRandom();
        StringBuilder stringBuilder = new StringBuilder();
        while (number > 0) {
            stringBuilder.delete(0, stringBuilder.length());
            for (int i = 0; i < 32; i++) {
                stringBuilder.append(AB.charAt(rnd.nextInt(AB.length())));
            }

            try {
                String queryIsExist = "select name from wells where name='" + stringBuilder.toString() + "'";
                ResultSet rs = statement.executeQuery(queryIsExist);
                if(rs.next()) {
                    continue;
                } else {
                    result.add(stringBuilder.toString());
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            number--;
        }
        return result;
    }

    private void addToExistingWell(String name, int numberForAdd) {
        try {
            // Добавление в существующую скважину
            String queryGetNumberOfWell = "select numberEquipment from wells where name='" + name + "'";
            ResultSet rs = statement.executeQuery(queryGetNumberOfWell);
            rs.next();
            int equipmentNumber = rs.getInt("numberEquipment") + numberForAdd;
            System.out.println(equipmentNumber);
            String stringUpdatedNumberOfName = "" +
                    "update wells " +
                    "set numberEquipment = " + equipmentNumber + " " +
                    "where name = '" + name + "'";
            statement.executeUpdate(stringUpdatedNumberOfName);
            // Добавление в существующую таблицу оборудования по имени скважины
            insertIntoEquipmentTable(numberForAdd, name);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private boolean isExist(String name) {
        try {
            Boolean result = false;
            String query = "select name from wells where name='" + name + "'";
            ResultSet rs = statement.executeQuery(query);
            if(rs.next())
                result = true;
            else
                result = false;
            return result;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    void close() throws SQLException {
        try {
            connection.close();
            reader.close();
            statement.close();
            System.out.println("Connection, reader, statement is closed.");
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }

    void showVewOfAllInfo() throws IOException, SQLException {
        try {
            System.out.println("Введите названия скважин через пробел :");
            String[] wellNames = reader.readLine().split(" ");
            if(wellNames.length == 0) {
                System.out.println("Вы не ввели 0 названий или отрицательное количество. Попробуйте снова.");
            }
            Map<String, String> namesAndNumberList = new HashMap<>();
            ResultSet rs = null;
            String query = "";
            for (int i = 0; i < wellNames.length; i++) {
                query = "select name, numberEquipment from wells where name='" + wellNames[i] +  "'";
                rs = statement.executeQuery(query);
                rs.next();
                namesAndNumberList.put(rs.getString("name"), rs.getString("numberEquipment"));
            }

            System.out.println("name \t number");
            for (Map.Entry<String, String> entry :
                    namesAndNumberList.entrySet()) {
                System.out.println(entry.getKey() + "\t" + entry.getValue());
            }
            
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    void exportInXML() {

    }

}
