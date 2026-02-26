package com.hybrid9.pg.Lipanasi.auth;




import com.hybrid9.pg.Lipanasi.dto.auth.UserDTO;
import com.hybrid9.pg.Lipanasi.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {
    @Autowired
    private UserRepository userRepository;

/*
    @GetMapping("/{id}")
    @PreAuthorize("#user.id == #id")
    public ResponseEntity user(@AuthenticationPrincipal AppUser user, @PathVariable Long id) {
        return ResponseEntity.ok(UserDTO.from(userRepository.findById(id).orElseThrow()));
    }
*/

    /*@GetMapping("/{id}")
    @ResponseBody
    ResponseEntity userInfo(@PathVariable Long id){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(!(authentication instanceof AnonymousAuthenticationToken)){
            return ResponseEntity.ok(UserDTO.from(userRepository.findById(id).orElseThrow()));
        }
        return null;
    }*/
}
