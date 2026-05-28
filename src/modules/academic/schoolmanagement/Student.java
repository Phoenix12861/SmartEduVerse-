package modules.academic.schoolmanagement;

public class Student {
    public int id, schoolId, classNumber, repeatClass;
    public String username, section, status;
    public double totalFees, feesPaid;

    public Student(int id, int sid, String u, int c, String s, String st, double tf, double fp, int rc) {
        this.id = id;
        this.schoolId = sid;
        this.username = u;
        this.classNumber = c;
        this.section = s;
        this.status = st;
        this.totalFees = tf;
        this.feesPaid = fp;
        this.repeatClass = rc;
    }
}
