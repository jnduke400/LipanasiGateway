package com.hybrid9.pg.Lipanasi.utilities;


import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;

//import net.minidev.json.JSONObject;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.InetAddress;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@AllArgsConstructor
public class PaymentUtilities {


    public HttpHeaders reportOutput(ByteArrayResource byteArrayResource, String fileType) {
        HttpHeaders responseHeaders = new HttpHeaders();
        try {
            responseHeaders.setContentLength(byteArrayResource.contentLength());
            responseHeaders.add("X-Frame-Options", "");
            if (fileType.equalsIgnoreCase("html")) {
                responseHeaders.setContentType(MediaType.valueOf("text/html"));
            } else if (fileType.equalsIgnoreCase("xlsx")) {
                responseHeaders.setContentType(MediaType.valueOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
                responseHeaders.put("Content-Disposition", Collections.singletonList("attachment; filename=PartnerTransactions.xlsx"));
            } else {
                responseHeaders.setContentType(MediaType.valueOf("application/pdf"));
                responseHeaders.put("Content-Disposition", Collections.singletonList("filename=PartnerTransactions.pdf"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return responseHeaders;
    }

    public HttpHeaders reportOutputError(String fileType) {
        HttpHeaders responseHeaders = new HttpHeaders();
        try {
            String invalidFile = "Invalid File";
            ByteArrayResource byteArrayResource = new ByteArrayResource(invalidFile.getBytes());
            responseHeaders.setContentLength(byteArrayResource.contentLength());
            responseHeaders.add("X-Frame-Options", "");
            if (fileType.equalsIgnoreCase("html")) {
                responseHeaders.setContentType(MediaType.valueOf("text/html"));
            } else if (fileType.equalsIgnoreCase("xlsx")) {
                responseHeaders.setContentType(MediaType.valueOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
                responseHeaders.put("Content-Disposition", Collections.singletonList("attachment; filename=invalid.xlsx"));
            } else {
                responseHeaders.setContentType(MediaType.valueOf("application/pdf"));
                responseHeaders.put("Content-Disposition", Collections.singletonList("inline; filename=invalid.pdf"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return responseHeaders;
    }

    public String sortableString(String search) {
        try {
            search = search.substring(search.lastIndexOf(".") + 1).replaceAll("([A-Z])", "_$1").toLowerCase();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return search;
    }

    public boolean isLong(String longValue) {
        boolean res = false;
        try {
            if (Long.parseLong(longValue) >= 0) {
                res = true;
            }
        } catch (Exception e) {
            res = false;
        }
        return res;
    }

    public boolean isDouble(String doubleValue) {
        boolean res = false;
        try {
            if (Double.parseDouble(doubleValue) >= 0) {
                res = true;
            }
        } catch (Exception e) {
            res = false;
        }
        return res;
    }

    public String getRandomNumber() {
        String res = "9999";
        try {
            double randomNum = Math.random() * 999999 + 1;
            String strRandom = Double.toString(randomNum);
            int intRandom = Integer.parseInt(strRandom.substring(0, strRandom.indexOf(".")));
            res = String.format("%06d", intRandom);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    public String toCapitalize(String sentence) {
        String capitalized = "";

        try {
            //Capitalize first character
            capitalized = sentence.substring(0, 1).toUpperCase() + sentence.substring(1);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return capitalized;
    }

    public String replaceChars(String word, String oldWord, String newWord) {
        String replaced = "";

        try {
            replaced = word.replace(oldWord, newWord);
        } catch (Exception e) {
            /**e.printStackTrace();*/
        }

        return replaced;
    }

    /*public String generateOrderNumber() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHH");
        return dateFormat.format(new Date()) + this.genReceiptNumber(5);
    }*/

    public String generateRefNumber() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHH");
        return dateFormat.format(new Date()) + this.genReceiptNumber(4);
    }

    public String generateOrangeCongoTransactionId() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHH");
        return "SC001" + dateFormat.format(new Date()) + this.genReceiptNumber(5);
    }

    public String generateAirtelRefNumber() {
        // SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyddHH");
        return "SC001" + this.genReceiptNumber(7);
    }

    /*public String generateTigopesaRefNumber() {
        // SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyddHH");
        return "SCP" + this.genReceiptNumber(9);
    }*/

    public String generateTigopesaRefNumber() {
        String prefix = "OYFOYP";

        LocalDateTime now = LocalDateTime.now();

        // Year and month: yyMM (e.g., 2506 for June 2025)
        String yearMonth = now.format(DateTimeFormatter.ofPattern("yyMM"));

        // Day and hour: ddHH (e.g., 0212 for 2nd day, 12 PM)
        String dayHour = now.format(DateTimeFormatter.ofPattern("ddHH"));

        // 4-digit random number
        String randomPart = String.format("%04d", new Random().nextInt(10000));

        // 15-digit numeric string from UUID
        String uuidDigits = UUID.randomUUID().toString().replaceAll("[^0-9]", "");
        String uuidPart = (uuidDigits + "000000000000000").substring(0, 15);

        return String.format("%s-%s-%s-%s-%s", prefix, yearMonth, dayHour, randomPart, uuidPart);
    }


    public String generateOrderNumber() {
        String prefix = "ORDER";

        LocalDateTime now = LocalDateTime.now();

        // Year and month: yyMM (e.g., 2506 for June 2025)
        String yearMonth = now.format(DateTimeFormatter.ofPattern("yyMM"));

        // Day and hour: ddHH (e.g., 0212 for 2nd day, 12 PM)
        String dayHour = now.format(DateTimeFormatter.ofPattern("ddHH"));

        // 4-digit random number
        String randomPart = String.format("%04d", new Random().nextInt(10000));

        // 15-digit numeric string from UUID
        String uuidDigits = UUID.randomUUID().toString().replaceAll("[^0-9]", "");
        String uuidPart = (uuidDigits + "000000000000000").substring(0, 15);

        return String.format("%s-%s-%s-%s-%s", prefix, yearMonth, dayHour, randomPart, uuidPart);
    }

    public String dateFormat(String dateString) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        String date_string = "";
        try {
            Date simple_date = sdf.parse(dateString);
            date_string = df.format(simple_date);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return date_string;
    }

    public String dateDefaultFormat(String dateString) {
        SimpleDateFormat df = new SimpleDateFormat("MM/dd/yy");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String date_string = "";
        try {
            Date simple_date = df.parse(dateString);
            date_string = sdf.format(simple_date);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return date_string;
    }

    public String dateTimeFormat(String dateString) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String date_string = "";
        try {
            Date simple_date = sdf.parse(dateString);
            date_string = df.format(simple_date);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return date_string;
    }

    public String textColors(String choices) {
        String text_color = "";

        try {
            Float choice = Float.parseFloat(choices);
            if (choice <= 20) {
                text_color = "danger";
            } else if (choice <= 50) {
                text_color = "warning";
            } else if (choice <= 80) {
                text_color = "info";
            } else {
                text_color = "success";
            }
        } catch (Exception e) {
            /**e.printStackTrace();*/
        }

        return text_color;
    }

    public boolean deleteFile(String file_name, String file_type, String file_folder) {
        boolean resp = false;
        try {
            File the_file = new File(file_folder + file_name + "." + file_type);
            if (!the_file.exists()) {
                resp = true;
            } else {
                if (the_file.delete()) {
                    resp = true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resp;
    }

    public String amountFormat(double amt) {
        String amount = "0";
        try {
            //double amt = Double.parseDouble(amount);
            DecimalFormat formatter = new DecimalFormat("#,###.00");
            amount = formatter.format(amt);
            if (amt == 0) {
                amount = "0.00";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return amount;
    }

    public int doubleToInteger(double values) {
        return (int) values;
    }

    public String amountFormatInt(String amount) {
        try {
            double amt = Double.parseDouble(amount);
            DecimalFormat formatter = new DecimalFormat("#,###");
            amount = formatter.format(amt);
            if (amt == 0) {
                amount = "0";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return amount;
    }

    public String latestTime(int days, Timestamp start_date) {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar c = Calendar.getInstance();

        try {
            c.setTime(sdf.parse(start_date.toString()));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        c.add(Calendar.DAY_OF_MONTH, days);
        String newDate = sdf.format(c.getTime());

        return newDate;
    }

    public String timeDifference(String GivenTime) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("Africa/Dar_es_Salaam"));
        String Differences = "";
        try {
            Date GivenTimes = sdf.parse(GivenTime);
            long Today = System.currentTimeMillis();
            long PostedDate = GivenTimes.getTime();
            long Difference = (Today - PostedDate);
            //System.out.println("Received Time is " + GivenTime + " Converted Time is " + GivenTimes);
            /*if (Difference >= 2419200000) {
                long Interval = Difference / (1000 * 60 * 60 * 24 * 28);
                if (Interval > 1) {
                    Differences = Interval + " Months Ago";
                } else {
                    Differences = Interval + " Month Ago";
                }
            } else */
            if (Difference >= 604800000) {
                long Interval = Difference / (1000 * 60 * 60 * 24 * 7);
                if (Interval > 1) {
                    Differences = Interval + " Weeks Ago";
                } else {
                    Differences = Interval + " Week Ago";
                }
            } else if (Difference >= 86400000) {
                long Interval = Difference / (1000 * 60 * 60 * 24);
                if (Interval > 1) {
                    Differences = Interval + " Days Ago";
                } else {
                    Differences = Interval + " Day Ago";
                }
            } else if (Difference >= 3600000) {
                long Interval = Difference / (1000 * 60 * 60);
                if (Interval > 1) {
                    Differences = Interval + " Hours Ago";
                } else {
                    Differences = Interval + " Hour Ago";

                }
            } else {
                long Interval = Difference / (1000 * 60);
                if (Interval > 1) {
                    Differences = Interval + " Minutes Ago";
                } else {
                    Differences = Interval + " Minute Ago";

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Differences;
    }

    public String trimLeadingZeros(String source) {
        String converted = "0";
        try {
            converted = source.replaceFirst("^0*", "");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return converted;
    }

    public String getSex(String sex) {
        try {
            if (sex.equalsIgnoreCase("male") || sex.equalsIgnoreCase("m") || sex.equalsIgnoreCase("mme") || sex.equalsIgnoreCase("me") || sex.equalsIgnoreCase("mwanaume") || sex.equalsIgnoreCase("man") || sex.equalsIgnoreCase("boy")) {
                sex = "Mme";
            } else {
                sex = "Mke";
            }
        } catch (Exception e) {
            e.printStackTrace();
            sex = "Mme";
        }
        return sex;
    }


    public String startDateParser(LocalDate localDate) {
        String formatedDate = null;
        if (String.valueOf(localDate.getMonthValue()).length() > 1) {
            formatedDate = localDate.getYear() + "-" + localDate.getMonthValue() + "-01 00:00:00";
        } else {
            formatedDate = localDate.getYear() + "-0" + localDate.getMonthValue() + "-01 00:00:00";
        }
        return formatedDate;
    }

    public String endDateParser(LocalDate localDate) {
        String formatedDate = null;
        if (String.valueOf(localDate.getMonthValue()).length() > 1) {
            formatedDate = localDate.getYear() + "-" + localDate.getMonthValue() + "-01 23:59:59";
        } else {
            formatedDate = localDate.getYear() + "-0" + localDate.getMonthValue() + "-01 23:59:59";
        }
        return formatedDate;
    }

    public Date parseDate(String dateStr) {
        Date parsedDate = new Date();
        SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            if (isParsable1(dateStr)) {
                parsedDate = sdf1.parse(dateStr);
            } else if (isParsable2(dateStr)) {
                parsedDate = sdf2.parse(dateStr);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return parsedDate;
    }

    private boolean isParsable1(String dateStr) {
        SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        sdf1.setLenient(false);
        try {
            sdf1.parse(dateStr);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private boolean isParsable2(String dateStr) {
        SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sdf2.setLenient(false);
        try {
            sdf2.parse(dateStr);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public String formatPhoneNumber(String dialCode, String phoneNumber) {
        String formattedNumber = phoneNumber;
        try {
            String validNumber = phoneNumber.replaceAll("\\D", "");
            String validDialCode = dialCode.replaceAll("\\D", "");
            int phoneLength = validNumber.length();
            if (phoneLength <= 9) {
                formattedNumber = validDialCode + validNumber;
            } else {
                formattedNumber = validDialCode + validNumber.substring(phoneLength - 9, phoneLength);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return formattedNumber;
    }


    public String getMonthName(String code) {
        String month = "January";
        try {
            if (code.equalsIgnoreCase("01")) {
                month = "January";
            } else if (code.equalsIgnoreCase("02")) {
                month = "February";
            } else if (code.equalsIgnoreCase("03")) {
                month = "March";
            } else if (code.equalsIgnoreCase("04")) {
                month = "April";
            } else if (code.equalsIgnoreCase("05")) {
                month = "May";
            } else if (code.equalsIgnoreCase("06")) {
                month = "June";
            } else if (code.equalsIgnoreCase("07")) {
                month = "July";
            } else if (code.equalsIgnoreCase("08")) {
                month = "August";
            } else if (code.equalsIgnoreCase("09")) {
                month = "September";
            } else if (code.equalsIgnoreCase("10")) {
                month = "October";
            } else if (code.equalsIgnoreCase("11")) {
                month = "November";
            } else if (code.equalsIgnoreCase("12")) {
                month = "December";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return month;
    }

    public String genMonthYear(String dateValue) {
        if (dateValue != null) {
            LocalDate date = LocalDate.parse(dateValue);
            String monthValue = String.format("%02d", date.getMonthValue());
            return date.getYear() + "-" + monthValue;
        } else {
            return "0000-00";
        }

    }


    public int genRandam(int min, int max) {
        Random random = new Random();
        return random.nextInt(max - min + 1) + min;
    }


    public String genReceiptNumber(int n) {
        // choose a Character random from this String
        String AlphaNumericString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                + "0123456789"
                + "abcdefghijklmnopqrstuvxyz";

        // create StringBuffer size of AlphaNumericString
        StringBuilder sb = new StringBuilder(n);

        for (int i = 0; i < n; i++) {

            // generate a random number between
            // 0 to AlphaNumericString variable length
            int index
                    = (int) (AlphaNumericString.length()
                    * Math.random());

            // add Character one by one in end of sb
            sb.append(AlphaNumericString
                    .charAt(index));
        }

        return sb.toString().toLowerCase();
    }

    public boolean isAlphanumeric(String controlNumber) {
        // Regex to check string is alphanumeric or not.
        String regex = "^(?=.*[a-zA-Z])(?=.*[0-9])[A-Za-z0-9]+$";

        // Compile the ReGex
        Pattern p = Pattern.compile(regex);

        // If the string is empty
        // return false
        if (controlNumber == null) {
            return false;
        }

        // Pattern class contains matcher() method
        // to find matching between given string
        // and regular expression.
        Matcher m = p.matcher(controlNumber);

        // Return if the string
        // matched the ReGex
        return m.matches();
    }


    public String localDateTimeToStringFormat(LocalDateTime localDateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        // Format the LocalDateTime object as a string

        return localDateTime.format(formatter);
    }

    public Timestamp convertToTimeStamp(String timeString) {
        // Define a DateTimeFormatter to parse the input string
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        // Parse the string to LocalDateTime
        LocalDateTime localDateTime = LocalDateTime.parse(timeString, formatter);

        // Convert LocalDateTime to Timestamp (if needed)
        return Timestamp.valueOf(localDateTime);
    }

    public String currentYearMonth() {
        LocalDate date = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        return date.format(formatter);
    }

    public boolean isNumeric(String textMessage) {
        if (textMessage == null) {
            return false;
        }
        try {
            double d = Double.parseDouble(textMessage);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    public String generateOrderToken() {
        String token = "";
        try {
            double randomNum = Math.random() * 999999 + 1;
            String strRandom = Double.toString(randomNum);
            int intRandom = Integer.parseInt(strRandom.substring(0, strRandom.indexOf(".")));
            token = String.format("%06d", intRandom) + genReceiptNumber(10);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return token;
    }

    public String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date());
    }

    public String getExpiryDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Calendar c = Calendar.getInstance();
        c.add(Calendar.HOUR, 1);
        return sdf.format(c.getTime());
    }

   /* public String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }*/

    public String getClientIp(HttpServletRequest request) {
        // List of headers to check in order of preference
        String[] headerNames = {
                "X-Forwarded-For",
                "X-Real-IP",
                "X-Forwarded",
                "X-Cluster-Client-IP",
                "CF-Connecting-IP"  // Cloudflare
        };

        for (String headerName : headerNames) {
            String ip = extractValidIpFromHeader(request.getHeader(headerName));
            if (ip != null) {
                return ip;
            }
        }

        // Fallback to remote address
        return request.getRemoteAddr();
    }

    private String extractValidIpFromHeader(String headerValue) {
        if (headerValue == null || headerValue.trim().isEmpty()) {
            return null;
        }

        // Split by comma and check each IP
        String[] ips = headerValue.split(",");
        for (String ip : ips) {
            String cleanIp = ip.trim();

            // Skip if unknown, invalid, or private/internal IP
            if (isValidPublicIp(cleanIp)) {
                return cleanIp;
            }
        }

        return null;
    }

    private boolean isValidPublicIp(String ip) {
        if (ip == null || ip.isEmpty() ||
                "unknown".equalsIgnoreCase(ip) ||
                "0.0.0.0".equals(ip) ||
                "127.0.0.1".equals(ip)) {
            return false;
        }

        try {
            InetAddress address = InetAddress.getByName(ip);

            // Check if it's a valid IP format and public address
            return !address.isLoopbackAddress() &&
                    !address.isLinkLocalAddress() &&
                    !address.isSiteLocalAddress() &&
                    !address.isAnyLocalAddress();

        } catch (Exception e) {
            return false;
        }
    }

    public String getOperatorPrefix(String msisdn) {
       return formatPhoneNumber("255",msisdn).substring(0,5);
    }


}
