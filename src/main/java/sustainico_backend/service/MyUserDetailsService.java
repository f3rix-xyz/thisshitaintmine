package sustainico_backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import sustainico_backend.Models.User;
import sustainico_backend.rep.UserRepository;

import java.util.ArrayList;

@Service
public class MyUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String contactNo) throws UsernameNotFoundException {
        User user = userRepository.findByContactNo(contactNo)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with contact no: " + contactNo));
        return new org.springframework.security.core.userdetails.User(user.getContactNo(), "", new ArrayList<>());
    }
}
