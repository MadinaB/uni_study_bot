import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.replykeyboard.ForceReplyKeyboard;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;

import GpaCalculator.*;

import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;

import java.util.logging.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.spi.DirStateFactory.Result;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;

import MakeSchedule.Course;
import MakeSchedule.Scheduler;

import java.util.*;

public class UniStudyBot extends TelegramLongPollingBot
{   
    private Logger logger = Logger.getLogger(UniStudyBot.class.getName());
    
    private static final int START_STATE = 0;
    private static final int MAIN_MENU = 1;
    // Course Settings BEGIN
    private static final int COURSE_SETTINGS = 2;
    private static final int ADDING_COURSE = 3;
    private static final int ADD_TIME = 4;
    private static final int ADDING_TIME = 5;
    private static final int DELETING_COURSE = 7;
    // Course Settings END
    private static final int GENERATING_NEW_SCHEDULE = 8;
    private static final int CALCULATE_GPA = 6;
    private static final int COUNT_GPA_NEW = 9;
    private static final int COUNT_GPA_CURRENT = 10;
    private static final int COUNTING_GPA_NEW = 11;
    private static final int COUNTING_GPA_CURRENT = 12;
    
    public String getStateFromInt(int state) {
        String result = "";
        switch(state) {
            case 0:
                result = "START_STATE";
                break;
            case 1:
                result = "MAIN_MENU";
                break;
            case 2:
                result = "COURSE_SETTINGS";
                break;
            case 3:
                result = "ADDING_COURSE";
                break;   
            case 4:
                result = "ADD_TIME";
                break;
            case 5:
                result = "ADDING_TIME";
                break;
            case 6:
                result = "CALCULATE_GPA";
                break;
            case 7:
                result = "DELETING_COURSE";
                break;       
            case 8:
                result = "GENERATING_NEW_SCHEDULE";
                break;
            case 9:
                result = "COUNT_GPA_NEW";
                break;
            case 10:
                result = "COUNT_GPA_CURRENT";
                break;
            case 11:
                result = "COUNTING_GPA_NEW";
                break;
            case 12:
                result = "COUNTING_GPA_CURRENT";
                break;
            default: 
                result = "YOU FORGOT TO ADD DESCRIPTION OF THE COMMAND!";
                break;
        }
        return result;
    }
    
    private SendMessage onCommandReceived(Message message) {
        SendMessage sendMessage = null;
        switch(message.getText().split(" ")[0])
        {
            case "/menu":
                sendMessage = menuSelected(message);
                break;
            case "/start":
                sendMessage = defaultSelected(message);
                break;
            case "/add_time":
                sendMessage = onAddTime(message);
                break;
            case "/view_courses":
                sendMessage = viewCoursesSelected(message);
                break;
            case "/calculate_gpa":
                sendMessage = calculateGpaSelected(message);
                break;
            case "/add_course":
                sendMessage = addCourseSelected(message);
                break;
            default:
                sendMessage = null;
                break;
        }
        return sendMessage;
    }
    
    /* State Pattern */
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Message message = update.getMessage();
            // Inserting user to database or updating his info
            Database.getInstance().addUser(message.getFrom().getId());
            
            System.out.println(message.getText()); // DEBUG ONLY
            
            // If a command received
            SendMessage sendMessage = onCommandReceived(message);
            if(sendMessage != null) {
                try{
                    sendMessage.setChatId(update.getMessage().getChatId());
                    execute(sendMessage);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                } 
                return;
            }
            
            // Getting user's current state
            final int state = Database.getInstance().getState(message.getFrom().getId(), message.getChatId());
            System.out.println(getStateFromInt(state)); // DEBUG ONLY
                                                
            switch(state) {
                case MAIN_MENU:
                    sendMessage = onMainMenu(message);
                    break;
                case COURSE_SETTINGS:
                    sendMessage = onCourseSettings(message);
                    break;
                case ADDING_COURSE:
                    sendMessage = onAddingCourse(message);
                    break;
                case ADD_TIME:
                    sendMessage = onAddTime(message);
                    break;
                case ADDING_TIME:
                    sendMessage = onAddingTime(message);
                    break;
                case DELETING_COURSE: // TODO
                    sendMessage = onDeletingCourse(message);
                    break;
                case GENERATING_NEW_SCHEDULE:
                    sendMessage = onGeneratingNewSchedule(message);
                    break;
                case CALCULATE_GPA:
                    sendMessage = onCalculateGpa(message);
                    break;
                case COUNT_GPA_NEW:
                    sendMessage = onSelectGpaScale(message, COUNT_GPA_NEW);
                    break;
                case COUNT_GPA_CURRENT:
                    sendMessage = onSelectGpaScale(message, COUNT_GPA_CURRENT);
                    break;
                case COUNTING_GPA_NEW:
                    sendMessage = onCountingGpaNew(message);
                    break;
                case COUNTING_GPA_CURRENT:
                    sendMessage = onCountingGpaCurrent(message);
                    break;
                default:
                    sendMessage = onDefault(message);
                    break;
            }
                    
            try {
                if(sendMessage == null) {
                    sendMessage = new SendMessage();
                    sendMessage.setText("Unknown option");
                }
                sendMessage.setChatId(update.getMessage().getChatId());
                execute(sendMessage); 
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
            
        }
    }
    
    private void sendErrorMessage(String errorMessage, Message message) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setReplyToMessageId(message.getMessageId()).setText(errorMessage);
        sendMessage.setChatId(message.getChatId());
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    
    private void sendInfoMessage(String infoMessage, Message message) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setText(infoMessage);
        sendMessage.setChatId(message.getChatId());
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    
    private List<String> splitInputAddEmptyStrings(Message message, int emptyStringsNum) {
        Splitter splitter = Splitter.on(',').trimResults();
        List<String> strings = Lists.newArrayList((splitter.splitToList(message.getText())));
        List<String> emptyStrings = new ArrayList<>();
        for(int i = 0; i < emptyStringsNum; i++) {
            emptyStrings.add("");
        }
        strings.addAll(emptyStrings);
        return strings;
    }
    
    private SendMessage onDefault(Message message) {
        if(message.getText().equals("/menu")) {
            return menuSelected(message);
        } else {
            return defaultSelected(message);
        }
    }
    
    private SendMessage onMainMenu(Message message) {
        if(message.getText().equals("/course_settings")) {
            return courseSettingsSelected(message);
        }
        else if(message.getText().equals("/generate_new_schedule")) {
            return generateNewScheduleSelected(message);
        }
        else if(message.getText().equals("/view_courses")) {
            return viewCoursesSelected(message);
        }
        else if(message.getText().equals("/calculate_gpa")) {
            return calculateGpaSelected(message);
        }
        return menuSelected(message);
    }
    
    private SendMessage onCalculateGpa(Message message) {
        SendMessage cancelMessage = cancelSelected(message, menuSelected(message));
        if(cancelMessage != null) return cancelMessage;
        
        if(message.getText().equals("/count_gpa_new")) {
            return selectGpaScaleSelected(message);
        } else if(message.getText().equals("/see_gpa_previous")) {
            // TODO
        } else if(message.getText().equals("/clear_gpa_data")) {
            // TODO
        } else if(message.getText().equals("/count_gpa_current")) {
            return selectGpaScaleSelected(message); // TODO
        } 
        return calculateGpaSelected(message);
    }
    
    private GpaCalculator getGpaCalculator(Message message) {
        String gpaScale = message.getReplyToMessage().getText();
        if(gpaScale == null) {
            sendErrorMessage("You must not cancel the force reply", message); // TODO better error message & check this call
            throw new IllegalStateException("No correct gpa scale was specified in the reply message");
        }
        GpaCalculator gpaCalculator = null;
        if(gpaScale.equals("4.0")) {
            gpaCalculator = new GpaFourZero();
        } else if(gpaScale.equals("4.3")) {
            gpaCalculator = new GpaFourThree();
        } else if(gpaScale.equals("100%")) {
            gpaCalculator = new GpaPercentage();
        } else {
            sendErrorMessage("Some error has occurred", message);
            throw new IllegalStateException("No correct gpa scale was specified in the reply message");
        }
        return gpaCalculator;
    }
    
    private void checkLimitsOfGpaData(List<String> courseInputs, Message message) {
        int gpa_sets = Database.getInstance().getNoOfGpaSets(message.getFrom().getId());
        System.out.println("gpa_sets_no: " + gpa_sets); // DEBUG ONLY
        if(gpa_sets > 10) {
            sendErrorMessage("The limit for number of user sets is 10\nPlease delete your GPA countings", message);
            throw new IllegalStateException("The limit of 10 gpa user sets has exceeded");
        }
        if(courseInputs.size() > 150) {
            sendErrorMessage("The limit for number of courses is 150", message);
            throw new IllegalStateException("The limit of 150 number of courses in the input has exceeded");
        }
    }
    
    private SendMessage cancelSelected(Message message, SendMessage toSend) {
        if(message.getText().equals("/cancel")) {
            try {
                SendMessage sendMessage = new SendMessage().setText("Cancelling..");
                sendMessage.setReplyMarkup(new ReplyKeyboardRemove());
                sendMessage.setChatId(message.getChatId());
                execute(sendMessage);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return toSend.setReplyToMessageId(null);
        }
        return null;
    }
    
    private SendMessage onCountingGpaNew(Message message) {
        SendMessage cancelMessage = cancelSelected(message, calculateGpaSelected(message));
        if(cancelMessage != null) return cancelMessage;
        try {
            Splitter splitter = Splitter.on(CharMatcher.whitespace()).trimResults().omitEmptyStrings();
            List<String> courseInputs = splitter.splitToList(message.getText());
            checkLimitsOfGpaData(courseInputs, message);
            GpaCalculator gpaCalculator = getGpaCalculator(message);
            List<Course> courses = new ArrayList<>();
            for(int i = 0; i < courseInputs.size(); i++) {
                Splitter splitter2 = Splitter.on(",").trimResults().omitEmptyStrings();
                List<String> courseInput = splitter2.splitToList(courseInputs.get(i));
                Course course = new Course();
                course.setCourseName(courseInput.get(0));
                course.setCredits(Integer.parseInt(courseInput.get(1)));
                if(! checkIfLetterGrade(courseInput.get(2))) {
                    sendErrorMessage("Some letter you specified is not correct", message);
                    throw new IllegalStateException("User-specified letters contain error");
                }
                course.setLetter(courseInput.get(2));
                courses.add(course);
            }
            gpaCalculator.setCourses(courses);
            double gpa = gpaCalculator.calculate();
            System.out.println(gpa); // DEBUG ONLY
            Database.getInstance().saveGpaSet(message.getFrom().getId(), gpa, courses);
            sendInfoMessage("Your overall GPA: " + gpa, message);
            return calculateGpaSelected(message);
        } catch(Exception e) { // TODO make an error format Exception
            logger.log(Level.WARNING, "This message was passed: " + message.getText() + "\n From user: " + message.getFrom().getId(), e);
        }
        return calculateGpaSelected(message);
    }
    
    private boolean checkIfLetterGrade(String string) {
        Pattern pattern = Pattern.compile("(?i)^([ABCD][-+]?|[F])$");
        Matcher matcher = pattern.matcher(string);
        return matcher.matches();
    }
    
    private SendMessage onCountingGpaCurrent(Message message) {
        SendMessage cancelMessage = cancelSelected(message, calculateGpaSelected(message));
        if(cancelMessage != null) return cancelMessage;
        List<Course> courses = Database.getInstance().getAllCourseNameCredits(message.getFrom().getId());
        if(courses.isEmpty()) {
            sendErrorMessage("You don't have any courses yet. Go to /add_course", message);
            return calculateGpaSelected(message);
        }
        try {
            Splitter splitter = Splitter.on(CharMatcher.whitespace()).trimResults().omitEmptyStrings();
            List<String> courseInputs = splitter.splitToList(message.getText());
            checkLimitsOfGpaData(courseInputs, message);
            GpaCalculator gpaCalculator = getGpaCalculator(message);
            if(courses.size() != courseInputs.size()) {
                sendErrorMessage("Looks like you didn't specify letter grades correctly", message);
                throw new IllegalArgumentException("Number of elements between courses and letter grades do not match");
            }
            for(int i = 0; i < courseInputs.size(); i++) {
                if(! checkIfLetterGrade(courseInputs.get(0))) {
                    sendErrorMessage("Some letter you specified is not correct", message);
                    throw new IllegalStateException("User-specified letters contain error");
                }
                courses.get(i).setLetter(courseInputs.get(i));
            }
            gpaCalculator.setCourses(courses);
            double gpa = gpaCalculator.calculate();
            System.out.println(gpa); // DEBUG ONLY
            Database.getInstance().saveGpaSet(message.getFrom().getId(), gpa, courses);
            sendInfoMessage("Your overall GPA: " + gpa, message);
            return calculateGpaSelected(message);
        } catch(Exception e) { // TODO make an error format Exception
            logger.log(Level.WARNING, "This message was passed: " + message.getText() + "\n From user: " + message.getFrom().getId(), e);
        }
        return calculateGpaSelected(message);
    }
    
    private SendMessage onSelectGpaScale(Message message, final int state) {
        SendMessage cancelMessage = cancelSelected(message, calculateGpaSelected(message));
        if(cancelMessage != null) return cancelMessage;
        
        String text = message.getText();
        if(! text.equals("4.0") && ! text.equals("4.3") && ! text.equals("100%")) {
            return calculateGpaSelected(message);
        } 
        if(state == COUNT_GPA_NEW) {
            return countingGpaNewSelected(message);
        } else if(state == COUNT_GPA_CURRENT) {
            return countingGpaCurrentSelected(message);
        }
        return calculateGpaSelected(message);
    }
    
    private SendMessage onCourseSettings(Message message) {
        SendMessage cancelMessage = cancelSelected(message, menuSelected(message));
        if(cancelMessage != null) return cancelMessage;
        
        if(message.getText().equals("/add_course")) {
            return addCourseSelected(message);
        } else if(message.getText().equals("/delete_course")) {
            return deleteCourseSelected(message); 
        } else if(message.getText().equals("/view_courses")) {
            return viewCoursesSelected(message); 
        }
        return courseSettingsSelected(message);
    }
    
    private SendMessage onGeneratingNewSchedule(Message message)
    {
        SendMessage cancelMessage = cancelSelected(message, menuSelected(message));
        if(cancelMessage != null) return cancelMessage;
        
        try {
            // TODO make preconditions checking
            SendMessage sendMessage = new SendMessage();
            String result = Scheduler.doWork(message.getText());
            sendMessage.setText(result);
            return sendMessage;
        } catch (Exception e) {
            e.printStackTrace(); // TODO
        }
        return menuSelected(message);
    }
    
    // TODO make courseName case-insensitive
    private SendMessage onAddingCourse(Message message) {
        SendMessage cancelMessage = cancelSelected(message, courseSettingsSelected(message));
        if(cancelMessage != null) return cancelMessage;
        
        try {
            List<String> strings = splitInputAddEmptyStrings(message, 4);
            String name = strings.get(0);
            int credits = Integer.parseInt(strings.get(1));
            Preconditions.checkArgument(! name.equals("") && name != null);
            Preconditions.checkArgument(credits > 0 && credits < 100);
            String professor = strings.get(2).equals("") ? null : strings.get(2);
            String room = strings.get(3).equals("") ? null : strings.get(3);
            Database.getInstance().addCourse(message.getFrom().getId(), name, credits, professor, room);
            // TODO Maybe setReplyToMessageId is not necessary
            return addTimeSelected(message, name).setText(name + " was successfully added");
        } catch(IllegalArgumentException e) {
            logger.log(Level.INFO, "This message was passed: " + message.getText() + "\nFrom user: " + message.getFrom().getId(), e);
            return addCourseSelected(message);
        }
        catch(Exception e) {
            logger.log(Level.WARNING, "This message was passed: " + message.getText() + "\n From user: " + message.getFrom().getId(), e);
        }
        // If something goes unexpected
        return courseSettingsSelected(message); 
    }
    
    private SendMessage onDeletingCourse(Message message) {
        SendMessage cancelMessage = cancelSelected(message, courseSettingsSelected(message));
        if(cancelMessage != null) return cancelMessage;
        
        try {
            List<String> strings = splitInputAddEmptyStrings(message, 0);
            Preconditions.checkArgument(strings.size() > 0);
            for(int i = 0; i < strings.size(); i++) {
                String name = strings.get(i);
                Preconditions.checkArgument(! name.equals("") && name != null);
                if(! Database.getInstance().existsCourseName(message.getFrom().getId(), name)) {
                    throw new NoSuchElementException();
                }
            }
            for(int i = 0; i < strings.size(); i++) {
                String name = strings.get(i);
                Database.getInstance().deleteCourse(message.getFrom().getId(), name);
            }
            return courseSettingsSelected(message).setText("Courses were successfully deleted");
        } catch (NoSuchElementException e) {
            logger.log(Level.INFO, "This message was passed: " + message.getText() + "\nFrom user: " + message.getFrom().getId(), e);
            sendErrorMessage("Course does not exist", message);
            return deleteCourseSelected(message);
        } catch(IllegalArgumentException e) {
            logger.log(Level.INFO, "This message was passed: " + message.getText() + "\nFrom user: " + message.getFrom().getId(), e);
            sendErrorMessage("Incorrect format", message);
            return deleteCourseSelected(message);
        }
        catch(Exception e) {
            logger.log(Level.WARNING, "This message was passed: " + message.getText() + "\n From user: " + message.getFrom().getId(), e);
        }
        // If something goes unexpected
        return courseSettingsSelected(message); 
    }
    
    private SendMessage onAddingTime(Message message) { // TODO make adding time with commas
        SendMessage cancelMessage = cancelSelected(message, courseSettingsSelected(message));
        if(cancelMessage != null) return cancelMessage;
        
        SendMessage sendMessage = new SendMessage();
        String courseName = null;
        Message reply;
        try {
            reply = message.getReplyToMessage();
            Preconditions.checkNotNull(reply);
            Preconditions.checkNotNull(reply.getText());
            Preconditions.checkArgument(! reply.getText().equals(""));
            System.out.println(reply.getText()); // DEBUG ONLY
            // TODO change to regular expressions
            courseName = reply.getText().split("\n")[0];
            courseName = courseName.split(": ")[1];
            Preconditions.checkNotNull(courseName);
            Preconditions.checkArgument(! courseName.equals(""));
            System.out.println(courseName); // DEBUG ONLY
        } catch (Exception e) {
            return courseSettingsSelected(message);
        }
        try {
            List<String> strings = splitInputAddEmptyStrings(message, 3);
            Preconditions.checkArgument(! strings.get(0).equals(""));
            String dayOfWeek = strings.get(0).toLowerCase();
            dayOfWeek = Character.toUpperCase(dayOfWeek.charAt(0)) + dayOfWeek.substring(1); // TODO unicode?
            System.out.println(dayOfWeek); // DEBUG ONLY
            DayOfWeek.valueOf(dayOfWeek.toUpperCase());
            String startTime = strings.get(1);
            String endTime = strings.get(2);
            if(! Pattern.matches("^([0-9]|0[0-9]|1[0-9]|2[0-3]):[0-5][0-9]$", startTime) ||
                    ! Pattern.matches("^([0-9]|0[0-9]|1[0-9]|2[0-3]):[0-5][0-9]$", endTime)) {
                throw new IllegalArgumentException();
            }
            startTime = startTime + ":00";
            endTime = endTime + ":00";
            System.out.println(startTime + " - " + endTime); // DEBUG ONLY
            Database.getInstance().addTime(message.getFrom().getId(), courseName, dayOfWeek, startTime, endTime); 
            return addTimeSelected(message, courseName).setText("Time was successfully added");
        } catch (IllegalArgumentException | NullPointerException e) {
            ForceReplyKeyboard forceReplyKeyboard = new ForceReplyKeyboard();
            sendErrorMessage("Incorrect format", message);
            sendMessage.setText(reply.getText()).setReplyMarkup(forceReplyKeyboard);
            return sendMessage;
        } 
        catch (Exception e) {
            logger.log(Level.WARNING, "This message was passed: " + message.getText() + "\n From user: " + message.getFrom().getId(), e);
        }
        return courseSettingsSelected(message);
    }
    
    /* /add_time courseName */
    private SendMessage onAddTime(Message message) { 
        SendMessage cancelMessage = cancelSelected(message, courseSettingsSelected(message));
        if(cancelMessage != null) return cancelMessage;
        
        if(message.getText().split(" ")[0].equals("/add_time")) {
            return addingTimeSelected(message);
        } 
        return null;
    }
    
    private SendMessage selectGpaScaleSelected(Message message) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setText("Select the GPA scale");
        sendMessage.setReplyMarkup(getGpaScaleKeyboard());
        if(message.getText().equals("/count_gpa_current")) {
            Database.getInstance().setState(message.getFrom().getId(), message.getChatId(), COUNT_GPA_CURRENT);
        } else {
            Database.getInstance().setState(message.getFrom().getId(), message.getChatId(), COUNT_GPA_NEW);
        }
        return sendMessage;
    }
    
    private SendMessage countingGpaNewSelected(Message message) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setReplyToMessageId(message.getMessageId());
        String info = "Write a list of courses in the following format:\n"
                + "COURSE_NAME,NUM_OF_CREDITS,LETTER_GRADE\n"
                + "Example: Calculus,3,A Philosophy,3,B+\n"
                + "Any whitespace characters between courses will do\n"
                + "Or write /cancel to go to previous menu";
        sendInfoMessage(info, message);
        ForceReplyKeyboard forceReplyKeyboard = new ForceReplyKeyboard();
        sendMessage.setReplyMarkup(forceReplyKeyboard).setText(message.getText());
        Database.getInstance().setState(message.getFrom().getId(), message.getChatId(), COUNTING_GPA_NEW);
        return sendMessage;
    }
    
    private SendMessage countingGpaCurrentSelected(Message message) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setReplyToMessageId(message.getMessageId());
        StringBuilder info = new StringBuilder("Write a letter grade for each of the following courses (in order) "
                + "by replacing question marks:\n");
        List<Course> courses = Database.getInstance().getAllCourseNameCredits(message.getFrom().getId());
        if(courses.isEmpty()) {
            sendErrorMessage("You don't have any courses yet. Go to /add_course", message);
            return calculateGpaSelected(message);
        }
        for(int i = 0; i < courses.size(); i++) {
            info.append(courses.get(i).getCourseName() + ", " + courses.get(i).getCredits() + " credits - ?" + "\n");
        }
        info.append("Example: A+ B- B A+\n");
        info.append("Or write /cancel to go to previous menu");
        sendInfoMessage(info.toString(), message);
        ForceReplyKeyboard forceReplyKeyboard = new ForceReplyKeyboard();
        sendMessage.setReplyMarkup(forceReplyKeyboard).setText(message.getText());
        Database.getInstance().setState(message.getFrom().getId(), message.getChatId(), COUNTING_GPA_CURRENT);
        return sendMessage;
    }
    
    private SendMessage viewCoursesSelected(Message message) {
        SendMessage sendMessage = new SendMessage();
        String courses = Database.getInstance().getAllCoursesAsString(message.getFrom().getId());
        if(courses == null) courses = "You don't have courses yet";
        sendMessage.setText(courses);
        final int state = Database.getInstance().getState(message.getFrom().getId(), message.getChatId());
        if(state == MAIN_MENU) {
            sendMessage.setReplyMarkup(getMenuKeyboard());
        } else if(state == COURSE_SETTINGS) {
            sendMessage.setReplyMarkup(getCourseSettingsKeyboard());
        }
        return sendMessage;
    }
    
    private SendMessage addingTimeSelected(Message message) {
        Splitter splitter = Splitter.on(' ').trimResults().limit(2);
        SendMessage sendMessage = new SendMessage();
        String courseName = null;
        try{
            courseName = splitter.splitToList(message.getText()).get(1);
            Preconditions.checkNotNull(courseName);
            Preconditions.checkArgument(! courseName.equals(""));
            if(! Database.getInstance().existsCourseName(message.getFrom().getId(), courseName)) {
                throw new NoSuchElementException();
            }
        } catch(IndexOutOfBoundsException | IllegalArgumentException | NullPointerException e) {
            sendMessage.setText("Incorrect format, should be /add_time <COURSE_NAME>");
            Database.getInstance().setState(message.getFrom().getId(), message.getChatId(), MAIN_MENU);
            return sendMessage;
        } catch (NoSuchElementException e) {
            sendMessage.setText("Such course does not exist. Please check your syntax");
            Database.getInstance().setState(message.getFrom().getId(), message.getChatId(), MAIN_MENU);
            return sendMessage;
        }
        ForceReplyKeyboard forceReplyKeyboard = new ForceReplyKeyboard();
        sendMessage.setText("You are adding time to course: " + courseName + "\n"
                + "Enter the time in the format: DAY,START_TIME,END_TIME\n"
                + "Required fields: DAY,START_TIME,END_TIME\n"
                + "Example: Monday,13:00,14:15\n"
                + "Or write /cancel to go to previous menu").setReplyMarkup(forceReplyKeyboard);
        Database.getInstance().setState(message.getFrom().getId(), message.getChatId(), ADDING_TIME);
        return sendMessage;
    }
    
    private SendMessage addTimeSelected(Message message, String courseName) {
        SendMessage sendMessage = new SendMessage();
        ReplyKeyboardMarkup replyMarkup = new ReplyKeyboardMarkup();
        replyMarkup.setOneTimeKeyboard(true);
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow keyboardRow = new KeyboardRow();
        keyboardRow.add("/add_time " + courseName);
        keyboardRows.add(keyboardRow);
        keyboardRow = new KeyboardRow();
        keyboardRow.add("/cancel");
        keyboardRows.add(keyboardRow);
        replyMarkup.setKeyboard(keyboardRows);
        sendMessage.setReplyMarkup(replyMarkup);
        Database.getInstance().setState(message.getFrom().getId(), message.getChatId(), ADD_TIME);
        return sendMessage;
    }
    
    private SendMessage addCourseSelected(Message message) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setText("Enter the course info in the format: COURSE_NAME,NUM_OF_CREDITS,PROFESSOR,ROOM\n"
                + "Required fields: COURSE_NAME,NUM_OF_CREDITS\n"
                + "Example: Calculus,3,Mark Zuckerberg,106-711\n"
                + "Or write /cancel to go to previous menu");
        ReplyKeyboardRemove replyMarkup = new ReplyKeyboardRemove();
        sendMessage.setReplyMarkup(replyMarkup);
        Database.getInstance().setState(message.getFrom().getId(), message.getChatId(), ADDING_COURSE);
        return sendMessage;
    }
    
    private SendMessage deleteCourseSelected(Message message) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setText("Enter the course name you want to delete, or a list of them\n"
                + "Example: Calculus,History,Fluid Mechanics\n"
                + "Write the exact name as written in /view_courses\n"
                + "Or write /cancel to go to previous menu");
        Database.getInstance().setState(message.getFrom().getId(), message.getChatId(), DELETING_COURSE);
        return sendMessage;
    }
    
    private SendMessage generateNewScheduleSelected(Message message) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setText("Enter the name of courses along with their respective schedule\n"
                + "Example: " // TODO
                + "Or write /cancel to go to previous menu");
        Database.getInstance().setState(message.getFrom().getId(), message.getChatId(), GENERATING_NEW_SCHEDULE);
        return sendMessage;
    }
    
    private ReplyKeyboardMarkup getCalculateGpaKeyboard() {
        ReplyKeyboardMarkup replyMarkup = new ReplyKeyboardMarkup();
        replyMarkup.setOneTimeKeyboard(true);
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow keyboardRow = new KeyboardRow();
        keyboardRow.add("/count_gpa_new");
        keyboardRows.add(keyboardRow);
        keyboardRow = new KeyboardRow();
        keyboardRow.add("/count_gpa_current");
        keyboardRows.add(keyboardRow);
        keyboardRow = new KeyboardRow();
        keyboardRow.add("/see_gpa_previous");
        keyboardRows.add(keyboardRow);
        keyboardRow = new KeyboardRow();
        keyboardRow.add("/clear_gpa_data");
        keyboardRows.add(keyboardRow);
        keyboardRow = new KeyboardRow();
        keyboardRow.add("/cancel");
        keyboardRows.add(keyboardRow);
        replyMarkup.setKeyboard(keyboardRows);
        return replyMarkup;
    }
    
    private ReplyKeyboardMarkup getGpaScaleKeyboard() {
        ReplyKeyboardMarkup replyMarkup = new ReplyKeyboardMarkup();
        replyMarkup.setOneTimeKeyboard(true);
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow keyboardRow = new KeyboardRow();
        keyboardRow.add("4.0");
        keyboardRows.add(keyboardRow);
        keyboardRow = new KeyboardRow();
        keyboardRow.add("4.3");
        keyboardRows.add(keyboardRow);
        keyboardRow = new KeyboardRow();
        keyboardRow.add("100%");
        keyboardRows.add(keyboardRow);
        keyboardRow = new KeyboardRow();
        keyboardRow.add("/cancel");
        keyboardRows.add(keyboardRow);
        replyMarkup.setKeyboard(keyboardRows);
        return replyMarkup;
    }
      
    private ReplyKeyboardMarkup getCourseSettingsKeyboard() {
        ReplyKeyboardMarkup replyMarkup = new ReplyKeyboardMarkup();
        replyMarkup.setOneTimeKeyboard(true);
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow keyboardRow = new KeyboardRow();
        keyboardRow.add("/add_course");
        keyboardRows.add(keyboardRow);
        keyboardRow = new KeyboardRow();
        keyboardRow.add("/delete_course");
        keyboardRows.add(keyboardRow);
        keyboardRow = new KeyboardRow();
        keyboardRow.add("/view_courses");
        keyboardRows.add(keyboardRow);
        keyboardRow = new KeyboardRow();
        keyboardRow.add("/cancel");
        keyboardRows.add(keyboardRow);
        replyMarkup.setKeyboard(keyboardRows);
        return replyMarkup;
    }
    
    private ReplyKeyboardMarkup getMenuKeyboard() {
        ReplyKeyboardMarkup replyMarkup = new ReplyKeyboardMarkup();
        replyMarkup.setOneTimeKeyboard(true);
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow keyboardRow = new KeyboardRow();
        keyboardRow.add("/course_settings");
        keyboardRows.add(keyboardRow);
        keyboardRow = new KeyboardRow();
        keyboardRow.add("/view_courses");
        keyboardRows.add(keyboardRow);
        keyboardRow = new KeyboardRow();
        keyboardRow.add("/generate_new_schedule");
        keyboardRows.add(keyboardRow);
        keyboardRow = new KeyboardRow();
        keyboardRow.add("/calculate_gpa");
        keyboardRows.add(keyboardRow);
        replyMarkup.setKeyboard(keyboardRows);
        return replyMarkup;
    }
    
    private SendMessage calculateGpaSelected(Message message) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setText("Select the menu item");
        sendMessage.setReplyMarkup(getCalculateGpaKeyboard());
        Database.getInstance().setState(message.getFrom().getId(), message.getChatId(), CALCULATE_GPA);
        return sendMessage;
    }
    
    private SendMessage courseSettingsSelected(Message message) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setText("Select the menu item");
        sendMessage.setReplyMarkup(getCourseSettingsKeyboard());
        Database.getInstance().setState(message.getFrom().getId(), message.getChatId(), COURSE_SETTINGS);
        return sendMessage;
    }
    
    private SendMessage menuSelected(Message message) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setText("Select the menu item");
        sendMessage.setReplyMarkup(getMenuKeyboard());
        Database.getInstance().setState(message.getFrom().getId(), message.getChatId(), MAIN_MENU);
        return sendMessage;
    }
    
    private SendMessage defaultSelected(Message message) {
        SendMessage sendMessage = new SendMessage();
        ReplyKeyboardRemove replyMarkup = new ReplyKeyboardRemove();
        sendMessage.setText("Welcome to our app, " + 
                (message.getFrom().getUserName() == null ? "" : message.getFrom().getUserName()) + 
                "!\nTo begin write /menu").setReplyMarkup(replyMarkup);
        Database.getInstance().setState(message.getFrom().getId(), message.getChatId(), START_STATE);
        return sendMessage;
    }
    
    @Override
    public String getBotUsername() {
        return Consts.USERNAME;
    }
    
    @Override
    public String getBotToken() {
        return Consts.TOKEN;
    }
}
