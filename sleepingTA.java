import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
public class sleepingTA{
private final static int DEFAULT_NUM_STUDENTS = 7;
private final static int WAITING_SIZE = 3;
static int[] waiting_room_size = new int[WAITING_SIZE];
Student[] students;
int students_waiting = 0;
int next_seat_spot = 0;
int next_teach_spot = 0;
int ta_sleeping_flag = 0;
Timer timer = new Timer();
Random rand = new Random();

public void runner(String[] args) {
    int i;
    int student_num;
    if (args.length != 0) {
        if (isInt(args[0] )){
            student_num = Integer.parseInt(args[0]);
            students = new Student[student_num];
        }
        else{
            System.out.println("Invalid input. Usage: java Driver <number of students>");
            return;
        }
    }
    else{
        student_num = DEFAULT_NUM_STUDENTS;
        students = new Student[DEFAULT_NUM_STUDENTS];
    }
    int[] student_ids = new int[student_num];
    TA ta = new TA();
    ta.start();
        for (i = 0; i < student_num; i++) {
            student_ids[i] = i + 1;
            students[i] = new Student(i + 1);
            students[i].start();
        }
        TimeOutTask timeOutTask = new TimeOutTask(ta, timer);
        timer.schedule(timeOutTask, 150000);
    
}

public static boolean isInt(String str) { 
    try {  
      Integer.parseInt(str);  
      return true;
    } catch(NumberFormatException e){  
      return false;  
    }  
  }
public static boolean isWaiting(int student_id){
    for(int i = 0; i < WAITING_SIZE; i++){
        if(waiting_room_size[i] == student_id){
            return true;
        }
    }
    return false;
}
  private class TA extends Thread{
    public void run(){
       System.out.println("Checking for students.");
        while(true){
            synchronized(waiting_room_size){            
            while(students_waiting == 0){
                try{
                    if (ta_sleeping_flag == 0){
                        System.out.println("No students in lobby. TA is sleeping.");
                        ta_sleeping_flag = 1;
                    }
                    waiting_room_size.wait();
                }
                catch(InterruptedException e){
                    for (Student student : students) {
                        student.interrupt();
                    }
                    return;
                }
            }
        }
                if(students_waiting > 0){
                    ta_sleeping_flag = 0;
                    int help_time = rand.nextInt(20000) + 5000;
                    System.out.println("TA is helping student "+  waiting_room_size[next_teach_spot]+ " for " + help_time/1000 + "s.");
                    //remove student from waiting area
                    int currentID = waiting_room_size[next_teach_spot];
                    waiting_room_size[next_teach_spot] = 0;
                    students_waiting--;
                    next_teach_spot = (next_teach_spot + 1) % WAITING_SIZE;
                    System.out.println("Students waiting: " + students_waiting + ".");
                     for (Student student : students) {
                            synchronized(student){
                        if(currentID != student.student_id){
                            student.notify();
                            }
                            }
                        }
                    try{
                        Thread.sleep(help_time);
                    }
                    catch(InterruptedException e){
                        for (Student student : students) {
                            student.interrupt();
                        }
                        return;
                    }                    
                }
        }
    }
  }
  private class Student extends Thread{
    int student_id;
    public Student(int id){
        this.student_id = id;
    }
    public void run(){
        while(true){
            synchronized(this){
            while (isWaiting(student_id)){
                try{
                    this.wait();
                }catch(InterruptedException e){
                    System.out.println("Student " + student_id + " has gone home.");
                    return;
                }
            }
        }
        synchronized(this){
            while(students_waiting >= WAITING_SIZE){
                System.out.println("No seats available. Student " + student_id + " will try later.");
            try{
                
                this.wait();
            }
            catch(InterruptedException e){
                System.out.println("Student " + student_id + " has gone home.");
                return;
            }
        }
        }
            int time = rand.nextInt(30000) + 15000 ;
            System.out.println("Student " + student_id + " is programming for " + time/1000 + "s.");
            try{
                Thread.sleep(time);
            }
            catch(InterruptedException e){
                System.out.println("Student " + student_id + " has gone home.");
                return;
            }     
            if(students_waiting < WAITING_SIZE){
                waiting_room_size[next_seat_spot] = student_id;
                students_waiting++;
                System.out.println("Student " + student_id + " sits down. " + students_waiting + " students waiting.");
                next_seat_spot = (next_seat_spot + 1) % WAITING_SIZE;
                //wake up TA
                synchronized(waiting_room_size){
                    waiting_room_size.notify();
                }
            }
        }

    }
  }
 private class TimeOutTask extends TimerTask{
    private TA thread;
    private Timer timer;
    public TimeOutTask(TA thread, Timer timer){
        this.thread = thread;
        this.timer = timer;
    }
    public void run(){
        if(thread != null && thread.isAlive()){
            thread.interrupt();
            timer.cancel();
            System.out.println("Office Hours are over. \nTA has gone home.");
        }
    }
  }
}