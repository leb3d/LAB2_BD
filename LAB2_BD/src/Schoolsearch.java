import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Scanner;

public class Schoolsearch {
    static class Student {
        String lastName;
        String firstName;
        int grade;
        int classroom;
        int bus;

        Student(String lastName, String firstName, int grade, int classroom, int bus) {
            this.lastName = lastName;
            this.firstName = firstName;
            this.grade = grade;
            this.classroom = classroom;
            this.bus = bus;
        }
        String toLine() {
            return lastName + "," + firstName + "," + grade + "," + classroom + "," + bus;
        }
    }

    static class Teacher {
        String lastName;
        String firstName;
        int classroom;
        Teacher(String lastName, String firstName, int classroom) {
            this.lastName = lastName;
            this.firstName = firstName;
            this.classroom = classroom;
        }
        String toLine() {
            return lastName + "," + firstName + "," + classroom;
        }
    }

    static ArrayList<Student> students = new ArrayList<>();
    static ArrayList<Teacher> teachers = new ArrayList<>();
    public static void main(String[] args) {
        long readStart = System.nanoTime();
        if (!loadStudents() || !loadTeachers()) return;
        long readEnd = System.nanoTime();
        // Load time is printed at startup, before any search query.
        System.out.printf(Locale.US, "Час завантаження: %.3fms%n", (readEnd - readStart) / 1_000_000.0);
        System.out.println("Для довідки введіть HELP.");

        try (Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.isEmpty()) continue;
                if (line.equalsIgnoreCase("Q") || line.equalsIgnoreCase("QUIT")) {
                    System.out.println("До побачення!");
                    return;
                }
                int colon = line.indexOf(':');
                String command = colon >= 0 ? line.substring(0, colon).trim().toUpperCase(Locale.ROOT)
                                            : line.toUpperCase(Locale.ROOT);
                String param = colon >= 0 ? line.substring(colon + 1).trim() : "";
                try {
                    switch (command) {
                        case "S": case "STUDENT":
                            if (colon < 0) { System.out.println("Формат: S: Прізвище [B]"); break; }
                            searchStudent(param); break;
                        case "T": case "TEACHER":
                            if (colon < 0) { System.out.println("Формат: T: Прізвище"); break; }
                            searchTeacher(param); break;
                        case "C": case "CLASSROOM":
                            if (colon < 0) { System.out.println("Формат: C: Номер [T]"); break; }
                            searchNumber(param, "C"); break;
                        case "G": case "GRADE":
                            if (colon < 0) { System.out.println("Формат: G: Номер [T]"); break; }
                            searchNumber(param, "G"); break;
                        case "B": case "BUS":
                            if (colon < 0) { System.out.println("Формат: B: Номер"); break; }
                            searchNumber(param, "B"); break;
                        case "A": case "ADD": addStudent(param); break;
                        case "AT": case "ADDTEACHER": addTeacher(param); break;
                        case "U": case "UPDATE": updateStudent(param); break;
                        case "D": case "DELETE": deleteStudent(param); break;
                        case "SAVE": saveData(param); break;
                        case "STATS": showStatistics(); break;
                        case "HELP": case "H": printHelp(); break;
                        default: System.out.println("Невідома команда. Введіть HELP.");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Помилка: очікувалося невід’ємне ціле число.");
                }
            }
        }
    }

    static boolean loadStudents() {
        Path file = Path.of("list.txt");
        if (!Files.isRegularFile(file)) {
            System.err.println("Помилка: немає " + file);
            return false;
        }
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            int row = 0;
            while ((line = reader.readLine()) != null) {
                row++;
                if (line.isBlank()) continue;
                String[] values = splitFields(line, 5, row, "list.txt");
                int grade = parseNonnegative(values[2]);
                int room = parseNonnegative(values[3]);
                int bus = parseNonnegative(values[4]);
                students.add(new Student(values[0], values[1], grade, room, bus));
            }
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Помилка читання list.txt: " + e.getMessage()); return false;
        }
        if (students.isEmpty()) {
            System.err.println("Помилка: файл list.txt порожній.");
            return false;
        }
        return true;
    }

    static boolean loadTeachers() {
        Path file = Path.of("teachers.txt");
        if (!Files.isRegularFile(file)) {
            System.err.println("Помилка: немає " + file);
            return false;
        }
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            int row = 0;
            while ((line = reader.readLine()) != null) {
                row++;
                if (line.isBlank()) continue;
                String[] values = splitFields(line, 3, row, "teachers.txt");
                teachers.add(new Teacher(values[0], values[1], parseNonnegative(values[2])));
            }
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Помилка читання teachers.txt: " + e.getMessage()); return false;
        }
        if (teachers.isEmpty()) {
            System.err.println("Помилка: файл teachers.txt порожній.");
            return false;
        }
        return true;
    }

    static String[] splitFields(String line, int count, int row, String file) {
        String[] parts = line.split(",", -1);
        if (parts.length != count) throw new IllegalArgumentException(file + ", рядок " + row + ": " + count + " полів");
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].trim();
            if (parts[i].isEmpty()) throw new IllegalArgumentException(file + ", рядок " + row + ": порожнє поле");
        }
        return parts;
    }

    static int parseNonnegative(String value) {
        int n = Integer.parseInt(value.trim());
        if (n < 0) throw new NumberFormatException("Від’ємне значення");
        return n;
    }

    static void output(ArrayList<String> lines, long started) {
        long ended = System.nanoTime();
        for (String s : lines) System.out.println(s);
        System.out.printf(Locale.US, "%.3fms%n", (ended - started) / 1_000_000.0);
    }

    static void searchStudent(String param) {
        String[] parts = param.trim().split("\\s+");
        if (parts.length < 1 || parts.length > 2 || parts[0].isBlank()
              || (parts.length == 2 && !parts[1].equalsIgnoreCase("B")
              && !parts[1].equalsIgnoreCase("Bus"))) {
            System.out.println("Формат: S: Прізвище [B]"); return;
        }
        boolean busOnly = parts.length == 2;
        String name = parts[0];
        ArrayList<String> lines = new ArrayList<>();
        long start = System.nanoTime();
        for (Student s : students) {
            if (!s.lastName.equals(name)) continue;
            if (busOnly) {
                lines.add(s.lastName + "," + s.firstName + "," + s.bus);
            } else {
                boolean found = false;
                for (Teacher t : teachers) {
                    if (s.classroom == t.classroom) {
                        lines.add(s.lastName + "," + s.firstName + "," + s.grade + "," +
                            s.classroom + "," + t.lastName + "," + t.firstName);
                        found = true;
                    }
                }
                if (!found) lines.add(s.lastName + "," + s.firstName + "," + s.grade + "," + s.classroom + ",-, -");
            }
        }
        output(lines, start);
    }

    static void searchTeacher(String name) {
        if (name.isEmpty() || name.contains(" ")) {
            System.out.println("Формат: T: Прізвище"); return;
        }
        ArrayList<String> lines = new ArrayList<>();
        long start = System.nanoTime();
        HashSet<Integer> classroomSet = new HashSet<>();
        for (Teacher t : teachers) if (t.lastName.equals(name)) classroomSet.add(t.classroom);
        for (Student s : students) {
            if (classroomSet.contains(s.classroom)) lines.add(s.lastName + "," + s.firstName);
        }
        output(lines, start);
    }

    static void searchNumber(String param, String kind) {
        String[] parts = param.trim().split("\\s+");
        if (parts.length < 1 || parts.length > 2 || parts[0].isEmpty()) {
            System.out.println("Формат: " + kind + ": Номер" + (kind.equals("B") ? "" : " [T]")); return;
        }
        boolean teacherOnly = parts.length == 2;
        if (teacherOnly && (kind.equals("B") || (!parts[1].equalsIgnoreCase("T")
              && !parts[1].equalsIgnoreCase("Teacher")))) {
            System.out.println("Формат: " + kind + ": Номер" + (kind.equals("B") ? "" : " [T]")); return;
        }
        int number = parseNonnegative(parts[0]);
        ArrayList<String> lines = new ArrayList<>();
        long start = System.nanoTime();
        if (kind.equals("C") && teacherOnly) {
            for (Teacher t : teachers) {
                if (t.classroom == number) lines.add(t.lastName + "," + t.firstName);
            }
        } else if (kind.equals("G") && teacherOnly) {
            HashSet<Integer> classrooms = new HashSet<>();
            for (Student s : students) if (s.grade == number) classrooms.add(s.classroom);
            HashSet<String> added = new HashSet<>();
            for (Teacher t : teachers) {
                String key = t.lastName + "\u0000" + t.firstName;
                if (classrooms.contains(t.classroom) && added.add(key))
                    lines.add(t.lastName + "," + t.firstName);
            }
        } else {
            for (Student s : students) {
                if ((kind.equals("C") && s.classroom == number)
                      || (kind.equals("G") && s.grade == number)
                      || (kind.equals("B") && s.bus == number)) {
                    if (kind.equals("B"))
                        lines.add(s.lastName + "," + s.firstName + "," + s.grade + "," + s.classroom);
                    else lines.add(s.lastName + "," + s.firstName);
                }
            }
        }
        output(lines, start);
    }

    static void addStudent(String param) {
        String[] f = splitInput(param, 5, "A: Прізвище,Ім'я,Grade,Classroom,Bus");
        if (f == null) return;
        long start = System.nanoTime();
        students.add(new Student(f[0], f[1], parseNonnegative(f[2]), parseNonnegative(f[3]), parseNonnegative(f[4])));
        outputMessage("Учня додано. ID: " + students.size(), start);
    }

    static void addTeacher(String param) {
        String[] f = splitInput(param, 3, "AT: Прізвище,Ім'я,Classroom");
        if (f == null) return;
        long start = System.nanoTime();
        teachers.add(new Teacher(f[0], f[1], parseNonnegative(f[2])));
        outputMessage("Викладача додано. ID: " + teachers.size(), start);
    }

    static String[] splitInput(String param, int count, String format) {
        String[] f = param.split(",", -1);
        if (f.length != count) { System.out.println("Формат: " + format); return null; }
        for (int i = 0; i < f.length; i++) {
            f[i] = f[i].trim();
            if (f[i].isEmpty()) { System.out.println("Поля не можуть бути порожніми."); return null; }
        }
        return f;
    }

    static void updateStudent(String param) {
        String[] f = splitInput(param, 3, "U: ID,Поле,НовеЗначення");
        if (f == null) return;
        int id = parseNonnegative(f[0]);
        if (id == 0 || id > students.size()) { System.out.println("Немає такого ID."); return; }
        String field = f[1].toUpperCase(Locale.ROOT);
        Student s = students.get(id - 1);
        // Parse first, mutate only after a valid value has been obtained.
        long start = System.nanoTime();
        switch (field) {
            case "LASTNAME": s.lastName = f[2]; break;
            case "FIRSTNAME": s.firstName = f[2]; break;
            case "GRADE": s.grade = parseNonnegative(f[2]); break;
            case "CLASSROOM": s.classroom = parseNonnegative(f[2]); break;
            case "BUS": s.bus = parseNonnegative(f[2]); break;
            default: System.out.println("Поле: LastName, FirstName, Grade, Classroom, Bus."); return;
        }
        outputMessage("Запис ID " + id + " оновлено.", start);
    }

    static void deleteStudent(String param) {
        int id = parseNonnegative(param);
        if (id == 0 || id > students.size()) { System.out.println("Немає такого ID."); return; }
        long start = System.nanoTime();
        students.remove(id - 1);
        outputMessage("Запис видалено. ID наступних записів змінилися.", start);
    }

    static void outputMessage(String message, long started) {
        long ended = System.nanoTime();
        System.out.println(message);
        System.out.printf(Locale.US, "%.3fms%n", (ended - started) / 1_000_000.0);
    }

    static void showStatistics() {
        long start = System.nanoTime();
        int n = students.size(), m = teachers.size();
        outputMessage("Учнів: " + n + "; викладачів (записів): " + m, start);
    }

    static void saveData(String param) {
        String value = param.toUpperCase(Locale.ROOT);
        if (!value.equals("TXT") && !value.equals("JSON") && !value.equals("XML")) {
            System.out.println("Формат: SAVE: TXT / JSON / XML"); return;
        }
        long start = System.nanoTime();
        try {
            if (value.equals("TXT")) {
                writeList(Path.of("list_export.txt"));
                writeTeachers(Path.of("teachers_export.txt"));
            } else if (value.equals("JSON")) {
                writeJson(Path.of("school_export.json"));
            } else {
                writeXml(Path.of("school_export.xml"));
            }
            outputMessage("Файли експорту збережено. Початкові list.txt і teachers.txt не змінено.", start);
        } catch (IOException e) {
            System.err.println("Помилка збереження: " + e.getMessage());
        }
    }

    static void writeList(Path path) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            for (Student s : students) { w.write(s.toLine()); w.newLine(); }
        }
    }
    static void writeTeachers(Path path) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            for (Teacher t : teachers) { w.write(t.toLine()); w.newLine(); }
        }
    }
    static String jsonEscape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
    static String xmlEscape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }
    static void writeJson(Path path) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            w.write("{\n  \"students\": [\n");
            for (int i = 0; i < students.size(); i++) {
                Student s = students.get(i);
                w.write("    {\"lastName\":\"" + jsonEscape(s.lastName) + "\",\"firstName\":\"" + jsonEscape(s.firstName)
                       + "\",\"grade\":" + s.grade + ",\"classroom\":" + s.classroom + ",\"bus\":" + s.bus + "}"
                       + (i + 1 < students.size() ? "," : "") + "\n");
            }
            w.write("  ],\n  \"teachers\": [\n");
            for (int i = 0; i < teachers.size(); i++) {
                Teacher t = teachers.get(i);
                w.write("    {\"lastName\":\"" + jsonEscape(t.lastName) + "\",\"firstName\":\""
                       + jsonEscape(t.firstName) + "\",\"classroom\":" + t.classroom + "}"
                       + (i + 1 < teachers.size() ? "," : "") + "\n");
            }
            w.write("  ]\n}\n");
        }
    }
    static void writeXml(Path path) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            w.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<school>\n  <students>\n");
            for (Student s : students) {
                w.write("    <student><lastName>" + xmlEscape(s.lastName) + "</lastName><firstName>" +
                        xmlEscape(s.firstName) + "</firstName><grade>" + s.grade + "</grade><classroom>" +
                        s.classroom + "</classroom><bus>" + s.bus + "</bus></student>\n");
            }
            w.write("  </students>\n  <teachers>\n");
            for (Teacher t : teachers) {
                w.write("    <teacher><lastName>" + xmlEscape(t.lastName) + "</lastName><firstName>" +
                        xmlEscape(t.firstName) + "</firstName><classroom>" + t.classroom + "</classroom></teacher>\n");
            }
            w.write("  </teachers>\n</school>\n");
        }
    }
    static void printHelp() {
        System.out.println("S: Прізвище       S: Прізвище B       T: Прізвище");
        System.out.println("C: Номер          B: Номер            G: Номер");
        System.out.println("C: Номер T        G: Номер T          STATS");
        System.out.println("A: Прізвище,Ім'я,Grade,Classroom,Bus");
        System.out.println("AT: Прізвище,Ім'я,Classroom");
        System.out.println("U: ID,Поле,НовеЗначення    D: ID");
        System.out.println("SAVE: TXT / JSON / XML     Q - вихід");
        System.out.println("ID студента - його номер у list.txt (починаючи з 1), викладача - у teachers.txt.");
    }
}
