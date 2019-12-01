import java.io.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Main {

    private static Date ParseDate(String stringDate,DateFormat df)
    {
        try {
            return df.parse(stringDate);
        } catch (ParseException e) {
            return null;
        }
    }

    private static Date FindDate(String[] words)
    {
        Date buff;
        return (buff=ParseDate(words[2]+" "+words[3], new SimpleDateFormat("dd.MM.yyyy HH:mm")))!=null ? buff
                : ParseDate(words[4]+" "+words[5],new SimpleDateFormat("dd.MM.yyyy HH:mm"));
    }

    private static String FindLost(String line)
    {
        String[] words=line.split(";"), words2;
        if(words[0].equals("NULL")^words[1].equals("NULL")) {
            try {
                BufferedReader DB = new BufferedReader(new InputStreamReader(new FileInputStream("database.csv"), "Cp1251"));
                DB.readLine();//строка заголовка

                Calendar date=Calendar.getInstance();
                date.setTime(FindDate(words));
                String Answer = null;
                boolean exclusive = false;
                byte lostColumn = words[0].equals("NULL") ? (byte) 0 : (byte) 1;//какое именно поле отсутствует

                while ((line = DB.readLine()) != null) {
                    words2 = line.split(";");
                    if (date.after(ParseDate(words2[2], new SimpleDateFormat("dd.MM.yyyy"))) && date.before(ParseDate(words2[3], new SimpleDateFormat("dd.MM.yyyy"))))
                    {
                        if (words[lostColumn].equals(words2[lostColumn])) {
                            Answer = words2[lostColumn];
                            if (exclusive) {
                                exclusive = false;
                                break;
                            } else
                                exclusive = true;
                        }
                    }
                }
                if (exclusive)
                    words[lostColumn]= Answer;
                DB.close();
                return String.join(";",words);
            }
            catch(IOException ex)
            {
                System.out.println("Ошибка открытия файлов. Проверьте целостность файла базы фамилий-номеров.");
                System.out.println(ex.toString());
            }
        }
        return line;
    }

    public static void main(String[] args) {
        try {
            BufferedReader Exemp = new BufferedReader(new InputStreamReader(new FileInputStream("Пример.csv"),"Cp1251"));
            BufferedWriter NormalDB = new BufferedWriter (new FileWriter("Answer.csv"));

            NormalDB.write(Exemp.readLine().trim()+"\n"); //Запись заголовков в файл ответа.

            ArrayList<String> devs = new ArrayList<>(); //недостающие устройства зоны
            ArrayList<String> queue = new ArrayList<>(); //очередь пропущенных строк

            String[] words,lineInWork=null;//массив для разбиения строки на слова
            String DevZone, line;
            boolean placeCheck,datacheck;

            Calendar date=Calendar.getInstance(), date2=Calendar.getInstance();

            boolean OnWork=false, WorkInQueue;
            int queuePos=0;

            while((line=Exemp.readLine())!=null || !queue.isEmpty()) {
                if(line!=null)
                    line = FindLost(line);//дозаполнение полей, если это возможно
                if(OnWork) {
                    if(queuePos<queue.size()) {
                        if(line!=null)
                            queue.add(line);
                        line = queue.get(queuePos);
                        WorkInQueue=true;
                    }
                    else
                        WorkInQueue=false;

                    words = line.split(";");

                    date2.setTime(FindDate(words));

                    if(java.lang.Math.abs(date2.getTimeInMillis()-date.getTimeInMillis())<=300000) { //проверка даты

                        placeCheck=devs.contains(words[10]);//проверка на искомое устройство
                        datacheck=(words[0].equals("NULL")^words[1].equals("NULL") ?
                                lineInWork[0].equals(words[0])||lineInWork[1].equals(words[1]) :
                                lineInWork[0].equals(words[0])&&lineInWork[1].equals(words[1]));//фамилия И номер, или, если одного из полей нет, Фамилия ИЛИ номер

                        if (placeCheck && datacheck)
                        {
                            NormalDB.write(line+"\n");
                            devs.remove(words[10]);//убрать из списка отчёт найденного устройства
                            if(WorkInQueue)
                                queue.remove(queuePos);//не забываем исключить из очереди
                            if(devs.isEmpty()) {
                                OnWork = false;
                                NormalDB.write("Completeness: True\n");
                                queuePos=0;
                            }
                        }
                        else {
                            if (!WorkInQueue)
                                queue.add(line);//пропущенная строка
                            queuePos++;
                        }
                    }
                    else {
                        if(!devs.isEmpty())
                            NormalDB.write("Completeness: False\n");
                        if(!WorkInQueue)
                            queue.add(line);//пропущенная строка
                        OnWork=false;
                        queuePos=0;
                    }
                }
                else {
                    if(!queue.isEmpty()) {
                        if(line!=null)
                            queue.add(line);
                        line=queue.get(0);
                        queue.remove(0);
                        }
                        lineInWork=line.split(";");

                        BufferedReader DBevent = new BufferedReader(new InputStreamReader(new FileInputStream("База для событий.csv"),"Cp1251"));
                        devs.clear();

                        date.setTime(FindDate(lineInWork));

                        while((DevZone=DBevent.readLine())!=null) {
                            words = DevZone.split(";");
                            if (Objects.equals(lineInWork[9], words[0]))//9-ый параметр - Зона
                                devs.add(words[1]);
                        }
                        devs.remove(lineInWork[10]);//убрать устройство, по отчёту которого мы ищем остальные

                        NormalDB.write(line+"\n");
                        OnWork=true;
                        DBevent.close();
                }
            }
            if(OnWork)
                NormalDB.write("Completeness: False");
            NormalDB.close();
            Exemp.close();
            System.out.println("Если ты это видишь: Ложки нет.");
        }
        catch(IOException ex)
        {
            System.out.println("Ошибка открытия файлов. Проверьте целостность файлов.");
            System.out.println(ex.toString());
        }
    }
}
