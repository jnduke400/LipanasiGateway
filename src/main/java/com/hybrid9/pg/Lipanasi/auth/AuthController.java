package com.hybrid9.pg.Lipanasi.auth;


import com.hybrid9.pg.Lipanasi.dto.auth.LoginDTO;
import com.hybrid9.pg.Lipanasi.dto.auth.SignupDTO;
import com.hybrid9.pg.Lipanasi.dto.auth.TokenDTO;
import com.hybrid9.pg.Lipanasi.entities.vendorx.Address;
import com.hybrid9.pg.Lipanasi.entities.vendorx.Business;
import com.hybrid9.pg.Lipanasi.entities.vendorx.MainAccount;
import com.hybrid9.pg.Lipanasi.entities.vendorx.VendorDetails;
import com.hybrid9.pg.Lipanasi.enums.*;
import com.hybrid9.pg.Lipanasi.entities.AppUser;
import com.hybrid9.pg.Lipanasi.entities.Role;
import com.hybrid9.pg.Lipanasi.entities.auth.Token;

import com.hybrid9.pg.Lipanasi.repositories.UserRepository;
import com.hybrid9.pg.Lipanasi.security.TokenGenerator;
import com.hybrid9.pg.Lipanasi.serviceImpl.UserManager;
import com.hybrid9.pg.Lipanasi.services.RoleService;
import com.hybrid9.pg.Lipanasi.services.auth.TokenService;
import com.hybrid9.pg.Lipanasi.services.vendorx.AddressService;
import com.hybrid9.pg.Lipanasi.services.vendorx.BusinessService;
import com.hybrid9.pg.Lipanasi.services.vendorx.VendorService;
import com.hybrid9.pg.Lipanasi.services.vendorx.MainAccountService;
import com.hybrid9.pg.Lipanasi.utilities.PaymentUtilities;
import jakarta.servlet.http.HttpServletRequest;
//import net.minidev.json.JSONObject;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.web.bind.annotation.*;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    UserDetailsManager userDetailsManager;
    @Autowired
    TokenGenerator tokenGenerator;
    @Autowired
    UserManager userManager;
    @Autowired
    private TokenService tokenService;
    @Autowired
    DaoAuthenticationProvider daoAuthenticationProvider;
    @Autowired
    @Qualifier("jwtRefreshTokenAuthProvider")
    JwtAuthenticationProvider refreshTokenAuthProvider;
    /* @Autowired
     private SessionManagementService sessionManagementService;*/
    @Autowired
    private PaymentUtilities paymentUtilities;
    @Autowired
    private RoleService roleService;
    @Autowired
    private MainAccountService mainAccountService;
    @Autowired
    private BusinessService businessService;
    @Autowired
    private AddressService addressService;
    @Autowired
    private VendorService vendorService;
    @Autowired
    private UserRepository userRepository;


    @PostMapping("/register")
    public ResponseEntity register(@RequestBody SignupDTO signupDTO) {

        Collection<Role> roleList = new ArrayList<>();
        this.roleService.findByName("ROLE_ADMIN").ifPresent(roleList::add);
        this.roleService.findByName("ROLE_USER").ifPresent(roleList::add);
        /* AppUser user = new AppUser(signupDTO.getUsername(), signupDTO.getPassword());*/

        AppUser userBuild = AppUser.builder()
                .username(signupDTO.getUsername())
                .password(signupDTO.getPassword())
                .firstname(signupDTO.getFirstname())
                .middlename(signupDTO.getMiddlename())
                .lastname(signupDTO.getLastname())
                .phoneNumber(signupDTO.getPhoneNumber())
                .address("address")
                /*.vendorx(vendorDetails1)*/
                .roles(roleList)
                .build();
        userDetailsManager.createUser(userBuild);

        Business business = Business.builder()
                .name(signupDTO.getBusiness().getName())
                .businessType(signupDTO.getBusiness().getBusinessType())
                .build();

        this.businessService.registerBusiness(business);
        Address address = Address
                .builder()
                .street(signupDTO.getAddress().getStreet())
                .city(signupDTO.getAddress().getCity())
                .state(signupDTO.getAddress().getState())
                .zip(signupDTO.getAddress().getZip())
                .country(signupDTO.getAddress().getCountry())
                .houseNumber(signupDTO.getAddress().getHouseNumber())
                .build();


        VendorDetails vendorDetails = VendorDetails
                .builder()
                .vendorName(signupDTO.getVendorDto().getVendorName())
                .vendorCode(signupDTO.getVendorDto().getVendorCode())
                .business(this.businessService.registerBusiness(business))
                .address(this.addressService.registerAddress(address))
                .hasCommission(signupDTO.getVendorDto().getHasCommission())
                .hasVat(signupDTO.getVendorDto().getHasVat())
                .charges(signupDTO.getVendorDto().getCharges())
                .status(VendorStatus.ACTIVE)
                .user((AppUser) userDetailsManager.loadUserByUsername(signupDTO.getUsername()))
                .billNumber(signupDTO.getVendorDto().getBillNumber())
                .build();

        VendorDetails vendorDetails1 = this.vendorService.registerVendorDetails(vendorDetails);

        MainAccount mainAccount = MainAccount.builder()
                .accountNumber(signupDTO.getVendorAccountDto().getAccountNumber())
                .accountName(signupDTO.getVendorAccountDto().getAccountName())
                .vendorDetails(vendorDetails1)
                .currentAmount(0)
                .desiredAmount(0)
                .withdrowAmount(0)
                .charges(0)
                .vendorCharges(0)
                .actualAmount(0)
                .build();


        this.mainAccountService.createMainAccount(mainAccount);
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(userBuild, signupDTO.getPassword(), Collections.EMPTY_LIST);

        if (!(authentication.getPrincipal() instanceof AppUser user)) {
            throw new BadCredentialsException(
                    MessageFormat.format("principal {0} is not of User type", authentication.getPrincipal().getClass())
            );
        }
        TokenDTO tokenDTO = tokenGenerator.createToken(authentication);
        this.extracted1(user, tokenDTO);

        return ResponseEntity.ok(tokenDTO);
    }

    private void extracted1(AppUser user, TokenDTO tokenDTO) {
        Token token = this.tokenService.findByUserId(user.getUsername());
        if (token != null) {
            token.setRefreshToken(tokenDTO.getRefreshToken());
//            token.setAccessToken(tokenDTO.getAccessToken());
            this.tokenService.updateToken(token);
        } else {
            Token newToken = new Token();
            newToken.setRefreshToken(tokenDTO.getRefreshToken());
//            newToken.setAccessToken(tokenDTO.getAccessToken());
            newToken.setUserId(user.getUsername());
            this.tokenService.addToken(newToken);
        }
    }

    @PostMapping("/login")
    @ResponseBody
    public ResponseEntity<String> login(@RequestBody LoginDTO loginDTO) {
        HttpStatus httpStatus = HttpStatus.OK;
        JSONObject response = new JSONObject();
        System.out.println("Session Id: " + loginDTO.getSessionId());
        //SessionManagement sessionManagement = this.sessionManagementService.findBySessionId(loginDTO.getSessionId());
        try {
            Authentication authentication = daoAuthenticationProvider.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(loginDTO.getUsername(), loginDTO.getPassword()));

            AppUser appUser = this.userManager.findUserByUsername(loginDTO.getUsername());

            TokenDTO tokenDTO = tokenGenerator.createToken(authentication);
            if (appUser == null) {
                this.extracted1(appUser, tokenDTO);
            }

            if (appUser != null) {
                response.put("accessToken", tokenDTO.getAccessToken());
                response.put("refreshToken", tokenDTO.getRefreshToken());
                /*response.put("username", appUser.getUsername());
                response.put("id", appUser.getId());*/
                //response.put("sessionId", sessionManagement.getSessionId());
                //response.put("remainingTime", this.evryMatrixUtilities.minutesDifference(sessionManagement.getExpirationTime()));
                /*response.put("roles", appUser.getRoles().stream().map(role -> role.getName()).collect(Collectors.toList()));*/
            } else {
                httpStatus = HttpStatus.NOT_FOUND;
                response.put("message", "User not found");
            }
        } catch (Exception e) {
            e.printStackTrace();
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
            response.put("message", e.getMessage());
        }
        return ResponseEntity.status(httpStatus).body(response.toString());
    }


    @PostMapping(value = "/login/paybill/prod", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<String> authenticate(@RequestBody LoginDTO loginDTO) {
        HttpStatus httpStatus = HttpStatus.OK;
        JSONObject response = new JSONObject();
        System.out.println("Session Id: " + loginDTO.getSessionId());
        //SessionManagement sessionManagement = this.sessionManagementService.findBySessionId(loginDTO.getSessionId());
        try {
            Authentication authentication = daoAuthenticationProvider.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(loginDTO.getUsername(), loginDTO.getPassword()));

            AppUser appUser = this.userManager.findUserByUsername(loginDTO.getUsername());

            TokenDTO tokenDTO = tokenGenerator.createToken(authentication);
            if (appUser == null) {
                this.extracted1(appUser, tokenDTO);
            }

            if (appUser != null) {
                response.put("accessToken", tokenDTO.getAccessToken());
                response.put("refreshToken", tokenDTO.getRefreshToken());
                /*response.put("username", appUser.getUsername());
                response.put("id", appUser.getId());*/
                //response.put("sessionId", sessionManagement.getSessionId());
                //response.put("remainingTime", this.evryMatrixUtilities.minutesDifference(sessionManagement.getExpirationTime()));
                /*response.put("roles", appUser.getRoles().stream().map(role -> role.getName()).collect(Collectors.toList()));*/
            } else {
                httpStatus = HttpStatus.NOT_FOUND;
                response.put("message", "User not found");
            }
        } catch (Exception e) {
            e.printStackTrace();
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
            response.put("message", e.getMessage());
        }
        return ResponseEntity.status(httpStatus).body(response.toString());
    }

    @PostMapping(value = "/login/paybill", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<String> authenticateUAT(@RequestBody LoginDTO loginDTO) {
        HttpStatus httpStatus = HttpStatus.OK;
        JSONObject response = new JSONObject();
        System.out.println("Session Id: " + loginDTO.getSessionId());
        //SessionManagement sessionManagement = this.sessionManagementService.findBySessionId(loginDTO.getSessionId());
        try {
            Authentication authentication = daoAuthenticationProvider.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(loginDTO.getUsername(), loginDTO.getPassword()));

            AppUser appUser = this.userManager.findUserByUsername(loginDTO.getUsername());

            TokenDTO tokenDTO = tokenGenerator.createToken(authentication);
            if (appUser == null) {
                this.extracted1(appUser, tokenDTO);
            }

            if (appUser != null) {
                response.put("accessToken", tokenDTO.getAccessToken());
                response.put("refreshToken", tokenDTO.getRefreshToken());
                /*response.put("username", appUser.getUsername());
                response.put("id", appUser.getId());*/
                //response.put("sessionId", sessionManagement.getSessionId());
                //response.put("remainingTime", this.evryMatrixUtilities.minutesDifference(sessionManagement.getExpirationTime()));
                /*response.put("roles", appUser.getRoles().stream().map(role -> role.getName()).collect(Collectors.toList()));*/
            } else {
                httpStatus = HttpStatus.NOT_FOUND;
                response.put("message", "User not found");
            }
        } catch (Exception e) {
            e.printStackTrace();
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
            response.put("message", e.getMessage());
        }
        return ResponseEntity.status(httpStatus).body(response.toString());
    }

    @PostMapping("/refresh")
    public ResponseEntity token(@RequestBody TokenDTO tokenDTO) {
        JSONObject response = new JSONObject();
        Authentication authentication = refreshTokenAuthProvider.authenticate(new BearerTokenAuthenticationToken(this.tokenService.getRefreshToken(tokenDTO.getUserId())));
        Jwt jwt = (Jwt) authentication.getCredentials();
        // check if present in db and not revoked, etc

        if (jwt != null) {
            Optional.ofNullable(this.userManager.findUserByUsername(jwt.getSubject())).ifPresent(user -> {
                response.put("accessToken", tokenGenerator.createToken(authentication).getAccessToken());
                response.put("refreshToken", tokenGenerator.createToken(authentication).getRefreshToken());
                response.put("username", user.getUsername());
                response.put("id", user.getId());
                response.put("roles", user.getRoles().stream().map(role -> role.getName()).collect(Collectors.toList()));
            });

        } else {
            response.put("message", "Invalid token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response.toString());
        }

        return ResponseEntity.status(HttpStatus.OK).body(response.toString());
    }

    @PostMapping("/logout")
    @ResponseBody
    public ResponseEntity<?> logout(@RequestBody TokenDTO tokenDTO) {
        if (tokenDTO.getAccessToken() != null) {
            // Perform any additional logout logic here
            this.tokenService.removeToken(tokenDTO.getAccessToken());


            // Invalidate the current user's authentication
            SecurityContextHolder.getContext().setAuthentication(null);
            return ResponseEntity.ok("Logout successful");
        } else {
            return ResponseEntity.badRequest().body("Invalid token");
        }
    }

    private String extractToken(HttpServletRequest request) {
        // Extract the token from the Authorization header
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // "Bearer " is 7 characters long
        }
        return null;
    }


}
