import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.*;
import MakeSchedule.Course;
import Objects.GpaSet;;

public class Database
{
    private Logger logger = Logger.getLogger(Database.class.getName());
    private static volatile Connection connection;
    private static volatile Database instance;
    
    private Database() {
        try {
            connection = DriverManager.getConnection(Consts.LINK_DB, Consts.USER_DB, Consts.PASSWORD_DB);
        } catch(SQLException e) {
            logger.log(Level.SEVERE, "Connection to the Database was not successful", e);
        }
        createInitialTables();
    }
    
    /* Singleton */
    public static Database getInstance() {
        Database localInstance = instance;
        if(localInstance == null) {
            synchronized (Database.class) {
                localInstance = instance;
                if(localInstance == null) {
                    instance = localInstance = new Database();
                }
            }
        }
        return instance;
    }
    
    private void createInitialTables() {
        try (Statement statement = connection.createStatement()) {
            logger.log(Level.INFO, "Creating (if not already) initial user table");
            statement.executeUpdate(CreateTablesStrings.CREATE_USERS_TABLE);
            logger.log(Level.INFO, "Creating (if not already) initial courses table");
            statement.executeUpdate(CreateTablesStrings.CREATE_COURSES_TABLE);
            logger.log(Level.INFO, "Creating (if not already) initial times table");
            statement.executeUpdate(CreateTablesStrings.CREATE_TIMES_TABLE);
            logger.log(Level.INFO, "Creating (if not already) initial states table");
            statement.executeUpdate(CreateTablesStrings.CREATE_STATES_TABLE);
            logger.log(Level.INFO, "Creating (if not already) initial gpa_sets table");
            statement.executeUpdate(CreateTablesStrings.CREATE_GPA_SETS_TABLE);
            logger.log(Level.INFO, "Creating (if not already) initial gpa_set_courses table");
            statement.executeUpdate(CreateTablesStrings.CREATE_GPA_SET_COURSES_TABLE);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Creation of initial tables was not successful", e);
        }
    }
    
    public void addUser(int userId) {
        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO users(user_id) VALUES(?) ON DUPLICATE KEY UPDATE last_activity=now()")) 
        {
            statement.setInt(1, userId);
            statement.executeUpdate();
            logger.log(Level.INFO, "Inserting/Updating user to the users table");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error occurred while adding to users table", e);
        }
    }
    
    public void addCourse(int userId, String name, int credits, String professor, String room) {
        try (PreparedStatement statement = connection.prepareStatement("INSERT IGNORE INTO courses(name,user_id,credits,professor,room) VALUES(?,?,?,?,?)")) 
        {
            statement.setString(1, name);
            statement.setInt(2, userId);
            statement.setInt(3, credits);
            if(professor == null) {
                statement.setNull(4, Types.VARCHAR);
            } else {
                statement.setString(4, professor);
            }
            if(room == null) {
                statement.setNull(5, Types.VARCHAR);
            } else {
                statement.setString(5, room);
            }
            statement.executeUpdate();
            logger.log(Level.INFO, "Inserting to the courses table");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error occurred while intesrting to the courses table", e);
        }
    }
    
    public void deleteCourse(int userId, String name) {
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM courses WHERE user_id=? AND name=?")) 
        {
            statement.setString(2, name);
            statement.setInt(1, userId);
            statement.execute();
            logger.log(Level.INFO, "Deleting courses from courses table");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error occurred while deleting rows from the courses table", e);
        }
    }
    
    public void addCourse(int userId, String name, int credits) {
        addCourse(userId, name, credits, null, null);
    }
    
    public int getState(int userId, Long chatId) {
        int state = 0;
        try(PreparedStatement statement = connection.prepareStatement("SELECT state FROM states WHERE user_id=? AND chat_id=?")) {
            statement.setInt(1, userId);
            statement.setLong(2, chatId);
            ResultSet resultSet = statement.executeQuery();
            if(resultSet.next()) {
                state = resultSet.getInt("state");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return state;
    }
    
    public void setState(int userId, Long chatId, int state) {
        try(PreparedStatement statement = connection.prepareStatement("REPLACE INTO states(user_id,chat_id,state) VALUES(?,?,?)")) {
            statement.setInt(1, userId);
            statement.setLong(2, chatId);
            statement.setInt(3, state);
            logger.log(Level.INFO, "Inserting/Updating to the states table");
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
  
    public boolean existsCourseName(int userId, String name) {
        try(PreparedStatement statement = connection.prepareStatement("SELECT name FROM courses WHERE user_id=? AND name=?")){
            statement.setInt(1, userId);
            statement.setString(2, name);
            ResultSet resultSet = statement.executeQuery();
            if(resultSet.next()) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public void addTime(int userId, String name, String day, String startTime, String endTime) {
        try (PreparedStatement statement = connection.prepareStatement("INSERT IGNORE INTO times(user_id,name,day,start_time,end_time) VALUES(?,?,?,?,?)")) 
        {
            statement.setInt(1, userId);
            statement.setString(2, name);
            statement.setString(3, day);
            statement.setString(4, startTime);
            statement.setString(5, endTime);
            logger.log(Level.INFO, "Inserting to the times table");
            statement.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error occurred while intesrting to the times table", e);
        }
    }
    
    public List<Course> getAllCourseNameCredits(int userId) {
        try(PreparedStatement statement = connection.prepareStatement("SELECT name,credits FROM courses "
                + "WHERE user_id=? ORDER BY name ASC")) {
            statement.setInt(1, userId);
            ResultSet resultSet = statement.executeQuery();
            List<Course> result = new ArrayList<>();
            while(resultSet.next()) {
                Course course = new Course(resultSet.getString("name"), resultSet.getInt("credits"));
                result.add(course);
            }
            return result;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public int getNoOfGpaSets(int userId) {
        try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) as total FROM gpa_sets WHERE user_id=?")) {
            statement.setInt(1, userId);
            logger.log(Level.INFO, "Getting count of rows from gpa_sets table");
            ResultSet resultSet = statement.executeQuery();
            if(resultSet.next()) { 
                return resultSet.getInt("total");
            } else {
                throw new SQLException("Couldn't select from gpa_sets, the next() returns false");
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error occurred while getting number of user gpa sets", e);
        }
        return -1;
    }
    
    public void clearGpaData(int userId) {
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM gpa_sets WHERE user_id=?")){
            statement.setInt(1, userId);
            statement.execute();
            logger.log(Level.INFO, "Deleting courses from gpa_sets table");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error occurred while deleting data from gpa_sets table", e);
        }
    }
    
    public void saveGpaSet(int userId, double gpa, String gpaScale, List<Course> courses) { // TODO pass GpaSet object
        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO gpa_sets(user_id,gpa,gpa_scale) VALUES(?,?,?)", Statement.RETURN_GENERATED_KEYS)) 
        {
            statement.setInt(1, userId);
            statement.setDouble(2, gpa);
            statement.setString(3, gpaScale);
            logger.log(Level.INFO, "Inserting to the gpa_sets table");
            int affectedRows = statement.executeUpdate();
            if(affectedRows == 0) {
                throw new SQLException("Cannot insert to gpa_sets, 0 rows affected");
            }
            ResultSet resultSet = statement.getGeneratedKeys();
            if(resultSet.next()) {
                int setId = resultSet.getInt(1); // TODO make as a transaction
                try (PreparedStatement statement2 = connection.prepareStatement("INSERT INTO gpa_set_courses(set_id,name,credits,letter) VALUES(?,?,?,?)")) {
                    for(int i = 0; i < courses.size(); i++) {
                        statement2.setInt(1, setId);
                        statement2.setString(2, courses.get(i).getCourseName());
                        statement2.setInt(3, courses.get(i).getCredits());
                        statement2.setString(4, courses.get(i).getLetter());
                        int affectedRows2 = statement2.executeUpdate();
                        if(affectedRows2 == 0) {
                            throw new SQLException("Cannot insert to gpa_set_courses, 0 rows affected");
                        }
                    }
                } catch (SQLException e) {
                    throw new SQLException("Failed to insert into gpa_set_courses. Error occurred.");
                }
            } else {
                throw new SQLException("Creating row in gpa_sets failed, no set_id obtained");
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error occurred saving GPA sets", e);
        }
    }
    
    public List<GpaSet> getGpaSets(int userId) { // TODO delete old timestamps 365 days
        List<GpaSet> gpaSets = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("SELECT gpa_sets.*,name,credits,letter FROM gpa_sets LEFT JOIN "
                + "gpa_set_courses on gpa_set_courses.set_id=gpa_sets.set_id WHERE gpa_sets.user_id=? ORDER BY gpa_sets.set_id", 
                ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)) {
            statement.setInt(1, userId);
            ResultSet resultSet = statement.executeQuery();
            while(resultSet.next()) {
                int setId = resultSet.getInt("set_id");
                GpaSet gpaSet = new GpaSet();
                gpaSet.setGpa(resultSet.getDouble("gpa"));
                gpaSet.setSetId(setId);
                gpaSet.setTimestamp(resultSet.getTimestamp("created"));
                gpaSet.setGpaScale(resultSet.getString("gpa_scale"));
                List<Course> courses = new ArrayList<>();
                resultSet.previous();
                while(resultSet.next()) {
                    if(setId != resultSet.getInt("set_id")) {
                        resultSet.previous();
                        break;
                    }
                    Course course = new Course();
                    course.setCourseName(resultSet.getString("name"));
                    course.setCredits(resultSet.getInt("credits"));
                    course.setLetter(resultSet.getString("letter"));
                    courses.add(course);
                }
                gpaSet.setCourses(courses);
                gpaSets.add(gpaSet);
            }
            return gpaSets;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return gpaSets;
    }
    
    public String getAllCoursesAsString(int userId) {
        try(PreparedStatement statement = connection.prepareStatement("SELECT courses.*,times.day,times.start_time,times.end_time FROM `courses` LEFT JOIN"
                + " `times` on courses.name=times.name and courses.user_id=times.user_id WHERE courses.user_id=? ORDER BY name ASC")) {
            statement.setInt(1, userId);
            ResultSet resultSet = statement.executeQuery();
            StringBuilder sb = new StringBuilder();
            String prev = "";
            while(resultSet.next()) {
                if(! prev.equals(resultSet.getString("name")) && ! prev.equals("")) {
                    sb.append("}\n");
                }
                if(! prev.equals(resultSet.getString("name"))) {
                    sb.append("{\n");
                    sb.append(resultSet.getString("name") + "\nCredits: " + resultSet.getString("credits") + "\n");
                    if(resultSet.getString("professor") != null) sb.append("Professor: " + resultSet.getString("professor") + "\n");
                    if(resultSet.getString("room") != null) sb.append("Room: " + resultSet.getString("room") + "\n");
                }
                if(resultSet.getString("day") != null) {
                    sb.append(resultSet.getString("day") + ", " + resultSet.getString("start_time").substring(0, 5) + " - "
                            + resultSet.getString("end_time").substring(0, 5) + "\n");
                }
                prev = resultSet.getString("name");
            }
            if(! prev.equals("")) {
                sb.append("}\n");
            }
            return (sb.toString().equals("") ? null : sb.toString());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
