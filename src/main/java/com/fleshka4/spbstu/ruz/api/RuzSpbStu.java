package com.fleshka4.spbstu.ruz.api;

import com.fleshka4.spbstu.ruz.api.models.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

public class RuzSpbStu {
    private static final String LINK = "https://ruz.spbstu.ru/api/v1/ruz/";

    public RuzSpbStu() {

    }

    private static JSONObject request(String requestLink) {
        try {
            URL ruz = new URL(requestLink);
            URLConnection yc = ruz.openConnection();
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(
                            yc.getInputStream()));
            String inputLine;
            StringBuilder result = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                result.append(inputLine);
            }
            JSONParser ruzPars = new JSONParser();
            JSONObject jsonObject = (JSONObject) ruzPars.parse(result.toString());
            in.close();
            return jsonObject;
        } catch (ParseException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // https://ruz.spbstu.ru/api/v1/ruz/faculties - получение списка кафедр (институтов)
    public static ArrayList<Faculty> getFaculties() {
        try {
            ArrayList<Faculty> answer = new ArrayList<>();
            JSONObject jsonObject = request(LINK + "faculties");
            JSONArray jsonArray = (JSONArray) Objects.requireNonNull(jsonObject).get("faculties");
            JSONParser jsonParser = new JSONParser();
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject ok = (JSONObject) jsonParser.parse(jsonArray.get(i).toString());
                answer.add(Faculty.parseJSON(ok));
            }
            return answer;
        } catch (ParseException parseException) {
            parseException.printStackTrace();
            return null;
        }
    }

    // https://ruz.spbstu.ru/api/v1/ruz/faculties/id - получение по id кафедры (института) названия
    public static Faculty getFacultyById(int id) {
        try {
            JSONObject jsonObject = request(LINK + "faculties/" + id);
            if (Objects.requireNonNull(jsonObject).get("error") == null) {
                return Faculty.parseJSON(jsonObject);
            } else {
                throw new RuzApiException(jsonObject.get("text").toString());
            }
        } catch (RuzApiException ruzApiException) {
            ruzApiException.printStackTrace();
            return null;
        }
    }

    // TODO: А нафига тут ArrayList. ArrayList заменить на List, либо вообще на Faculty
    // Получение факультета по имени, смысла в массиве не вижу...
    public static ArrayList<Faculty> searchFacultiesByName(String name) {
        ArrayList<Faculty> faculties = getFaculties();
        ArrayList<Faculty> result = new ArrayList<>();
        for (int i = 0; i < Objects.requireNonNull(faculties).size(); i++) {
            if (faculties.get(i).getName().toLowerCase(Locale.ROOT).contains(name.toLowerCase(Locale.ROOT)) ||
                    faculties.get(i).getAbbr().toLowerCase(Locale.ROOT).contains(name.toLowerCase(Locale.ROOT))) {
                result.add(faculties.get(i));
            }
        }
        return result;
    }

    // https://ruz.spbstu.ru/api/v1/ruz/faculties/id/groups - группы, пренадлежащие институту по его id
    public static ArrayList<Group> getGroupsByFacultyId(int id) {
        try {
            if (Objects.requireNonNull(request(LINK + "faculties/" + id)).get("error") != null) {
                throw new RuzApiException(Objects.requireNonNull(request(LINK +
                        "faculties/" + id)).get("text").toString());
            }
            return getGroupArrayList(request(LINK + "faculties/" + id + "/groups"));
        } catch (RuzApiException ruzApiException) {
            ruzApiException.printStackTrace();
            return null;
        }
    }

    // TODO: 2 одинаковых запроса в 1 функции
    // https://ruz.spbstu.ru/api/v1/ruz/search/groups?q=name - поиск группы по имени
    public static ArrayList<Group> searchGroupsByName(String name) {
        try {
            if (Objects.requireNonNull(request(LINK + "search/groups?q=" + name)).get("groups") == null) {
                throw new RuzApiException("Групп не найдено");
            }
            return getGroupArrayList(request(LINK + "search/groups?q=" + name));
        } catch (RuzApiException ruzApiException) {
            ruzApiException.printStackTrace();
            return null;
        }
    }

    // TODO: функция совсем не паблик должна быть
    // Парсит список групп в массив
    public static ArrayList<Group> getGroupArrayList(JSONObject jsonObject) {
        try {
            ArrayList<Group> answer = new ArrayList<>();
            JSONArray jsonArray = (JSONArray) Objects.requireNonNull(jsonObject).get("groups");
            JSONParser jsonParser = new JSONParser();
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject ok = (JSONObject) jsonParser.parse(jsonArray.get(i).toString());
                answer.add(Group.parseJSON(ok));
            }
            return answer;
        } catch (ParseException parseException) {
            parseException.printStackTrace();
            return null;
        }
    }

    // Список всех преподавателей
    // https://ruz.spbstu.ru/api/v1/ruz/teachers
    public static ArrayList<Teacher> getTeachers() {
        return getTeacherArrayList(request(LINK + "teachers"));
    }

    // Поиск преподавателя по id
    // https://ruz.spbstu.ru/api/v1/ruz/teachers/id
    public static Teacher getTeacherById(int id) {
        try {
            JSONObject jsonObject = request(LINK + "teachers/" + id);
            if (Objects.requireNonNull(jsonObject).get("error") == null) {
                return Teacher.parseJSON(jsonObject);
            } else {
                throw new RuzApiException(jsonObject.get("text").toString());
            }
        } catch (RuzApiException ruzApiException) {
            ruzApiException.printStackTrace();
            return null;
        }
    }

    // Поиск учителя по части имени/фамилии/отчеству/full_name (заменить пробелы на %20 при запросе)
    // https://ruz.spbstu.ru/api/v1/ruz/search/teachers?q=name
    public static ArrayList<Teacher> searchTeachersByName(String name) {
        name = name.replace(" ", "%20");
        try {
            if (Objects.requireNonNull(request(LINK + "search/teachers?q=" + name))
                    .get("teachers") == null) {
                throw new RuzApiException("Ни один преподаватель не найден");
            }
            return getTeacherArrayList(request(LINK + "search/teachers?q=" + name));
        } catch (RuzApiException ruzApiException) {
            ruzApiException.printStackTrace();
            return null;
        }
    }

    // Преобразует json резултат из учителей в список учителей
    public static ArrayList<Teacher> getTeacherArrayList(JSONObject jsonObject) {
        try {
            ArrayList<Teacher> answer = new ArrayList<>();
            JSONArray jsonArray = (JSONArray) Objects.requireNonNull(jsonObject).get("teachers");
            JSONParser jsonParser = new JSONParser();
            if (jsonArray == null) return null;
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject ok = (JSONObject) jsonParser.parse(jsonArray.get(i).toString());
                answer.add(Teacher.parseJSON(ok));
            }
            return answer;
        } catch (ParseException exception) {
            exception.printStackTrace();
            return null;
        }
    }

    // https://ruz.spbstu.ru/api/v1/ruz/buildings - список "сооружений"/корпусов. (Обратите внимание что в нем куча мусорных значений)
    public static ArrayList<Building> getBuildings() {
        try {
            ArrayList<Building> answer = new ArrayList<>();
            JSONObject jsonObject = request(LINK + "buildings");
            JSONArray jsonArray = (JSONArray) Objects.requireNonNull(jsonObject).get("buildings");
            JSONParser jsonParser = new JSONParser();
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject ok = (JSONObject) jsonParser.parse(jsonArray.get(i).toString());
                answer.add(Building.parseJSON(ok));
            }
            return answer;
        } catch (ParseException parseException) {
            parseException.printStackTrace();
            return null;
        }
    }

    // https://ruz.spbstu.ru/api/v1/ruz/buildings/id - получение сооружения по id
    public static Building getBuildingById(int id) {
        try {
            JSONObject jsonObject = request(LINK + "buildings/" + id);
            if (Objects.requireNonNull(jsonObject).get("error") == null) {
                return Building.parseJSON(jsonObject);
            } else {
                throw new RuzApiException(jsonObject.get("text").toString());
            }
        } catch (RuzApiException ruzApiException) {
            ruzApiException.printStackTrace();
            return null;
        }
    }

    // Поиск сооружения по имени, спользуя простое совпадение значений
    public static ArrayList<Building> searchBuildingsByName(String name) {
        ArrayList<Building> buildings = getBuildings();
        ArrayList<Building> result = new ArrayList<>();
        for (int i = 0; i < Objects.requireNonNull(buildings).size(); i++) {
            if (buildings.get(i).getName().toLowerCase(Locale.ROOT).contains(name.toLowerCase(Locale.ROOT)) ||
                    buildings.get(i).getAbbr().toLowerCase(Locale.ROOT).contains(name.toLowerCase(Locale.ROOT))) {
                result.add(buildings.get(i));
            }
        }
        return result;
    }

    // Поиск аудитории по имени
    public static ArrayList<Auditory> searchAuditoriesByName(String name) {
        ArrayList<Auditory> result = new ArrayList<>();
        ArrayList<Building> buildings = getBuildings();
        for (int i = 0; i < Objects.requireNonNull(buildings).size(); i++) {
            ArrayList<Room> rooms = Objects.requireNonNull(getAuditoriesByBuildingId(buildings.get(i).getId())).getRooms();
            ArrayList<Room> temp = new ArrayList<>();
            for (int j = 0; j < Objects.requireNonNull(rooms).size(); j++) {
                if (rooms.get(j).getName().toLowerCase(Locale.ROOT).contains(name.toLowerCase(Locale.ROOT))) {
                    temp.add(rooms.get(j));
                }
            }
            if (temp.size() > 0) {
                result.add(new Auditory(temp, buildings.get(i)));
            }
        }
        return result;
    }

    // Поиск сооружения по id аудитории путем перебора всех id аудиторий во всех зданиях
    public static Building findBuildingByAuditoryId(int id) {
        ArrayList<Building> buildings = getBuildings();
        for (int i = 0; i < Objects.requireNonNull(buildings).size(); i++) {
            ArrayList<Room> rooms = Objects.requireNonNull(getAuditoriesByBuildingId(buildings.get(i).getId())).getRooms();
            for (int j = 0; j < Objects.requireNonNull(rooms).size(); j++) {
                if (id == rooms.get(j).getId())
                    return buildings.get(i);
            }
        }
        return null;
    }

    // https://ruz.spbstu.ru/api/v1/ruz/buildings/id/rooms - аудитории по id здания
    public static Auditory getAuditoriesByBuildingId(int id) {
        try {
            if (Objects.requireNonNull(request(LINK + "buildings/" + id)).get("error") != null) {
                throw new RuzApiException(Objects.requireNonNull(request(LINK + "buildings/" + id))
                        .get("text").toString());
            }
            JSONObject jsonObject = request(LINK + "buildings/" + id + "/rooms");
            return new Auditory(getRooms(jsonObject),
                    Building.parseJSON((JSONObject) Objects.requireNonNull(jsonObject).get("building")));
        } catch (RuzApiException ruzApiException) {
            ruzApiException.printStackTrace();
            return null;
        }
    }

    public static Auditory getAuditories(JSONObject jsonObject) {
        try {
            JSONArray jsonArray = (JSONArray) jsonObject.get("auditories");
            JSONParser jsonParser = new JSONParser();
            jsonObject = (JSONObject) ((JSONObject) jsonParser.parse(jsonArray.get(0).toString())).get("building");
            return new Auditory(parseAuditories(jsonArray),
                    Building.parseJSON(jsonObject));
        } catch (ParseException parseException) {
            parseException.printStackTrace();
            return null;
        }
    }

    private static ArrayList<Room> parseAuditories(JSONArray jsonArray) {
        return parseRooms(jsonArray);
    }

    private static ArrayList<Room> parseRooms(JSONArray jsonArray) {
        try {
            ArrayList<Room> answer = new ArrayList<>();
            JSONParser jsonParser = new JSONParser();
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject ok = (JSONObject) jsonParser.parse(jsonArray.get(i).toString());
                answer.add(Room.parseJSON(ok));
            }
            return answer;
        } catch (ParseException parseException) {
            parseException.printStackTrace();
            return null;
        }
    }

    public static ArrayList<Room> getRooms(JSONObject jsonObject) {
        JSONArray jsonArray = (JSONArray) Objects.requireNonNull(jsonObject).get("rooms");
        return parseRooms(jsonArray);
    }

    public static ArrayList<Lesson> getLessons(JSONObject jsonObject) {
        try {
            ArrayList<Lesson> answer = new ArrayList<>();
            JSONArray jsonArray = (JSONArray) Objects.requireNonNull(jsonObject).get("lessons");
            JSONParser jsonParser = new JSONParser();
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject ok = (JSONObject) jsonParser.parse(jsonArray.get(i).toString());
                answer.add(Lesson.parseJSON(ok));
            }
            return answer;
        } catch (ParseException parseException) {
            parseException.printStackTrace();
            return null;
        }
    }

    public static ArrayList<Day> getDays(JSONObject jsonObject) {
        try {
            ArrayList<Day> answer = new ArrayList<>();
            JSONArray jsonArray = (JSONArray) Objects.requireNonNull(jsonObject).get("days");
            JSONParser jsonParser = new JSONParser();
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject ok = (JSONObject) jsonParser.parse(jsonArray.get(i).toString());
                answer.add(Day.parseJSON(ok));
            }
            return answer;
        } catch (ParseException parseException) {
            parseException.printStackTrace();
            return null;
        }
    }

    // https://ruz.spbstu.ru/api/v1/ruz/scheduler/id Получение расписания по id группы
    public static Schedule getScheduleByGroupId(int id) {
        try {
            JSONObject jsonObject = request(LINK + "scheduler/" + id);
            if (Objects.requireNonNull(jsonObject).get("error") == null) {
                return Schedule.parseJSON(jsonObject);
            } else {
                throw new RuzApiException(jsonObject.get("text").toString() + "а");
            }
        } catch (RuzApiException ruzApiException) {
            ruzApiException.printStackTrace();
            return null;
        }
    }

    // https://ruz.spbstu.ru/api/v1/ruz/scheduler/id?date=date Получение расписания по id группы на неделю с определенной датой
    public static Schedule getScheduleByGroupIdAndDate(int id, LocalDate date) {
        try {
            JSONObject jsonObject = request(LINK + "scheduler/" + id +
                    "?date=" + date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            if (Objects.requireNonNull(jsonObject).get("error") == null) {
                return Schedule.parseJSON(jsonObject);
            } else {
                throw new RuzApiException(jsonObject.get("text").toString() + "а");
            }
        } catch (RuzApiException ruzApiException) {
            ruzApiException.printStackTrace();
            return null;
        }
    }

    // https://ruz.spbstu.ru/api/v1/ruz/teachers/id/scheduler Раписание учителя на неделю по его id
    public static Schedule getScheduleByTeacherId(int id) {
        try {
            JSONObject jsonObject = request(LINK + "teachers/" + id + "/scheduler");
            if (Objects.requireNonNull(jsonObject).get("error") == null) {
                return Schedule.parseJSON(jsonObject);
            } else {
                throw new RuzApiException(jsonObject.get("text").toString());
            }
        } catch (RuzApiException ruzApiException) {
            ruzApiException.printStackTrace();
            return null;
        }
    }

    // https://ruz.spbstu.ru/api/v1/ruz/teachers/id/scheduler Раписание учителя на неделю с днем date по его id
    public static Schedule getScheduleByTeacherIdAndDate(int id, LocalDate date) {
        try {
            JSONObject jsonObject = request(LINK + "teachers/" + id +
                    "/scheduler?date=" + date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            if (Objects.requireNonNull(jsonObject).get("error") == null) {
                return Schedule.parseJSON(jsonObject);
            } else {
                throw new RuzApiException(jsonObject.get("text").toString());
            }
        } catch (RuzApiException ruzApiException) {
            ruzApiException.printStackTrace();
            return null;
        }
    }

    // Функция ищет расписание по адуитории
    // В основе лежит запрос формата: https://ruz.spbstu.ru/api/v1/ruz/buildings/id_building/rooms/id_room/scheduler
    // Где id_building - id здания, а id_room - id комнаты
    public static Schedule getScheduleByAuditoryId(int id) {
        try {
            JSONObject jsonObject = request(LINK + "buildings/" +
                    findBuildingByAuditoryId(id) + "/rooms/" + id + "/scheduler");
            if (Objects.requireNonNull(jsonObject).get("error") == null) {
                return Schedule.parseJSON(jsonObject);
            } else {
                throw new RuzApiException(jsonObject.get("text").toString());
            }
        } catch (RuzApiException ruzApiException) {
            ruzApiException.printStackTrace();
            return null;
        }
    }

    // Аналогично функции выше, только с датой
    public static Schedule getScheduleByAuditoryIdAndDate(int id, LocalDate date) {
        try {
            JSONObject jsonObject = request(LINK + "buildings/" +
                    findBuildingByAuditoryId(id) + "/rooms/" + id + "/scheduler?date=" +
                    date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            if (Objects.requireNonNull(jsonObject).get("error") == null) {
                return Schedule.parseJSON(jsonObject);
            } else {
                throw new RuzApiException(jsonObject.get("text").toString());
            }
        } catch (RuzApiException ruzApiException) {
            ruzApiException.printStackTrace();
            return null;
        }
    }
}
