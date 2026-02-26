package com.hybrid9.pg.Lipanasi.serviceImpl;



import com.hybrid9.pg.Lipanasi.configs.CustomRoutingDataSource;
import com.hybrid9.pg.Lipanasi.entities.vendorx.VendorDetails;
import com.hybrid9.pg.Lipanasi.enums.VendorStatus;
import com.hybrid9.pg.Lipanasi.entities.AppUser;
import com.hybrid9.pg.Lipanasi.entities.Role;
import com.hybrid9.pg.Lipanasi.entities.vendorx.Address;
import com.hybrid9.pg.Lipanasi.entities.vendorx.Business;
import com.hybrid9.pg.Lipanasi.entities.vendorx.MainAccount;
import com.hybrid9.pg.Lipanasi.repositories.UserRepository;
import com.hybrid9.pg.Lipanasi.services.AppUserService;
import com.hybrid9.pg.Lipanasi.services.vendorx.AddressService;
import com.hybrid9.pg.Lipanasi.services.vendorx.BusinessService;
import com.hybrid9.pg.Lipanasi.services.vendorx.MainAccountService;
import com.hybrid9.pg.Lipanasi.services.vendorx.VendorService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

@Service
@AllArgsConstructor
public class UserManager implements UserDetailsManager, AppUserService {
    private final UserRepository userRepository;
    private final RoleServiceImpl roleService;
    /*private final AccountServiceImpl accountService;
    private final SmsResource smsResource;*/
    PasswordEncoder passwordEncoder;
    private final MainAccountService mainAccountService;
    private final VendorService vendorService;
    private final BusinessService businessService;
    private final AddressService addressService;

    @Transactional
    @Override
    public void createUser(UserDetails user) {
        CustomRoutingDataSource.setCurrentDataSource("primary");
        ((AppUser) user).setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save((AppUser) user);
        CustomRoutingDataSource.clearCurrentDataSource();

    }

    @Transactional
    @Override
    public void updateUser(UserDetails user) {

    }

    @Transactional
    @Override
    public void deleteUser(String username) {

    }

    @Transactional
    @Override
    public void changePassword(String oldPassword, String newPassword) {

    }
    @Transactional(readOnly = true)
    @Override
    public boolean userExists(String username) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        boolean exists = userRepository.existsByUsername(username);
        CustomRoutingDataSource.clearCurrentDataSource();
        return exists;
    }
    @Transactional(readOnly = true)
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        CustomRoutingDataSource.setCurrentDataSource("replica");

        AppUser appUser = Optional.ofNullable(userRepository.findByUsername(username))
                .orElseThrow(() -> new UsernameNotFoundException(
                        MessageFormat.format("username {0} not found", username)
                ));
        CustomRoutingDataSource.clearCurrentDataSource();
        return appUser;

    }


    @Transactional
    public void setupDefaultUser() {
        CustomRoutingDataSource.setCurrentDataSource("primary");
        if (this.userRepository.count() == 0) {
            Collection<Role> roleList = new ArrayList<>();
            Role role_1 = new Role("ROLE_ADMIN"); //this.roleService.createRole("ROLE_ADMIN");
            Role role_2 = new Role("ROLE_USER"); //this.roleService.createRole("ROLE_USER");
            roleList.add(role_1);
            roleList.add(role_2);
            AppUser user = new AppUser(passwordEncoder.encode("sC0op!"), "Scoop", "", "Shopping", "255688044555", "schoop@shoppingcart.co.tz", "Masaki", roleList);
            AppUser appUser = this.userRepository.save(user);

            Business business = Business
                    .builder()
                    .name("Online Shopping")
                    .businessType("Sales")
                    .build();

            VendorDetails vendorDetails = VendorDetails
                    .builder()
                    .vendorName("Scoop")
                    .vendorCode("10001")
                    .business(this.businessService.registerBusiness(business))
                    .address(this.addressService.registerAddress(Address
                            .builder()
                            .street("Address Building")
                            .city("Dar Es Salaam")
                            .state("Kinondoni")
                            .zip("5440995")
                            .country("Tanzania")
                            .houseNumber("10")
                            .build()))
                    .hasCommission("false")
                    .hasVat("false")
                    .user(appUser)
                    .charges(0)
                    .status(VendorStatus.ACTIVE)
                    .billNumber("300300")
                    .build();


            MainAccount mainAccount = MainAccount.builder()
                    .accountNumber("100019802001")
                    .accountName("Scoop Account")
                    .vendorDetails(this.vendorService.registerVendorDetails(vendorDetails))
                    .currentAmount(0)
                    .desiredAmount(0)
                    .withdrowAmount(0)
                    .charges(0)
                    .vendorCharges(0)
                    .actualAmount(0)
                    .build();

            this.mainAccountService.createMainAccount(mainAccount);
            System.out.println("user created " + appUser.getUsername());
            System.out.println("xyzzz "+this.userRepository.findByUsername(appUser.getUsername()).getUsername());
        }
        CustomRoutingDataSource.clearCurrentDataSource();
    }

  @Transactional
    @Override
    public AppUser addUser(AppUser user) {
        CustomRoutingDataSource.setCurrentDataSource("primary");
        AppUser appUser = this.userRepository.save(user);
        CustomRoutingDataSource.clearCurrentDataSource();
        return appUser;
    }

    @Transactional(readOnly = true)
    @Override
    public AppUser findUserByUsername(String username) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        AppUser appUser = this.userRepository.findByUsername(username);
        CustomRoutingDataSource.clearCurrentDataSource();
        return appUser;
    }
    @Transactional(readOnly = true)
    @Override
    public Page<AppUser> findAll(int page, int size, String sort, String order) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        if (order.equals("asc")) {
            Page<AppUser> pageResult = this.userRepository.findAll(PageRequest.of(page, size, Sort.by(sort).ascending()));
            CustomRoutingDataSource.clearCurrentDataSource();
            return pageResult;
        } else {
            Page<AppUser> pageResult = this.userRepository.findAll(PageRequest.of(page, size, Sort.by(sort).descending()));
            CustomRoutingDataSource.clearCurrentDataSource();
            return pageResult;
        }

    }
    @Transactional(readOnly = true)
    @Override
    public Optional<AppUser> findByPhoneNumber(String phoneNumber) {
        CustomRoutingDataSource.setCurrentDataSource("replica");
        Optional<AppUser> appUser = this.userRepository.findByPhoneNumber(phoneNumber);
        CustomRoutingDataSource.clearCurrentDataSource();
        return appUser;
    }


}