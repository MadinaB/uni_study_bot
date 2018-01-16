package MakeSchedule;

import java.util.ArrayList;

public class Course {
    private String courseName;
    private ArrayList<TimeSlotOptions> courseTime;

    public Course(){
        this.courseTime = new ArrayList<>();
    }
    public Course(String courseName, ArrayList<TimeSlotOptions> timeSlotOptions){
        this.courseName = courseName;
        this.courseTime = timeSlotOptions;
    }
    public void addTimeSlotOption(TimeSlotOptions timeSlotOption) {
        courseTime.add(timeSlotOption);
    }

    public void addToTimeSlotOption(int i, String day, Time time) {
        this.courseTime.get(i).addTimeToTimeSlot(day, time);
    }


    public ArrayList<TimeSlotOptions> getCourseTime() {
        return courseTime;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public void setCourseTime(ArrayList<TimeSlotOptions> courseTime) {
        this.courseTime = courseTime;
    }
}
