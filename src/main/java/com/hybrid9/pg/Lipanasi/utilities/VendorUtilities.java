package com.hybrid9.pg.Lipanasi.utilities;

import com.hybrid9.pg.Lipanasi.entities.vendorx.MainAccount;
import com.hybrid9.pg.Lipanasi.services.vendorx.MainAccountService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
@AllArgsConstructor
public class VendorUtilities {
    private final MainAccountService accountService;
    public String genAccountNumber() {
        String accNumb = null;

        MainAccount mainAccount = this.accountService.findTopAccounts();
        String genNumb = genUserNumber(mainAccount.getAccountNumber());
        accNumb = "9902" + genNumb;

        System.out.println("Account Number: " + accNumb);

        return accNumb;
    }

    private String genUserNumber(String numb) {
        int parsedNumb = Integer.parseInt(numb.substring(4));
        String numbStr = parsedNumb + 1 + "";
        StringBuilder builder = new StringBuilder();
        builder.append("0".repeat(Math.max(0, 6 - numbStr.length())));
        builder.append(numbStr);
        System.out.println("Number: " + builder.toString());
        return builder.toString();
    }

    public String genVendorCode() {
        String code = "";
        try {
            double randomNum = Math.random() * 999 + 1;
            String strRandom = Double.toString(randomNum);
            int intRandom = Integer.parseInt(strRandom.substring(0, strRandom.indexOf(".")));
            code = "GTL"+String.format("%03d", intRandom);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return code;
    }

    public String getBillNumber() {
        double randomNum = Math.random() * 9999 + 1;
        String strRandom = Double.toString(randomNum);
        int intRandom = Integer.parseInt(strRandom.substring(0, strRandom.indexOf(".")));
        return intRandom+"";
    }

    private static final String[] DOMAINS = {
            "example.com", "mail.com", "test.org", "demo.net"
    };

    public static String generateRandomEmail() {
        String alphabet = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        String chars = alphabet + alphabet.toUpperCase() + digits;
        Random random = new Random();

        StringBuilder localPart = new StringBuilder();
        int length = 8 + random.nextInt(5); // 8 to 12 characters

        for (int i = 0; i < length; i++) {
            localPart.append(chars.charAt(random.nextInt(chars.length())));
        }

        String domain = DOMAINS[random.nextInt(DOMAINS.length)];

        return localPart.toString() + "@" + domain;
    }
}
