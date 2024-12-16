package sustainico_backend.service;

import com.google.firebase.auth.FirebaseToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import  org.springframework.stereotype.Service;
import sustainico_backend.Models.User;
import sustainico_backend.rep.UserRepository;
import sustainico_backend.util.ApiResponse;
import sustainico_backend.util.JwtUtil;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private final UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    public UserService(UserRepository userRepository){
        this.userRepository = userRepository;
    }

    public User saveUser(User user){
        return userRepository.save(user);
    }

    public User getUserById(String UserId){
        return userRepository.findByUserId(UserId);
    }

    public Optional<User> getUserByContactNo(String contactNo) {
        return userRepository.findByContactNo(contactNo);
    }

    public Optional<User> getExistingUser(String contactNo) {
        return getUserByContactNo(contactNo);
    }

    public User createUser(User user) {
        Optional<User> existingUser = getExistingUser(user.getContactNo());
        if (existingUser.isPresent()) {
            User oldUser = existingUser.get();
            oldUser.setNewUser(false);
            return oldUser;
        }
        user.setNewUser(true);
        return userRepository.save(user);
    }
    public String generateToken(User user) {
        return jwtUtil.generateToken(user.getUserId());
    }

    public Optional<User> updateUser(User user) {
        Optional<User> existingUser = Optional.ofNullable(userRepository.findByUserId(user.getUserId()));
        if (existingUser.isPresent()) {
            User updateUser = existingUser.get();
            updateUser.setUserName(user.getUserName());
            updateUser.setEmail(user.getEmail());
            return Optional.of(userRepository.save(updateUser));
        } else {
            return Optional.empty();
        }
    }

    public boolean deleteUser(String contactNo) {
        Optional<User> existingUser = userRepository.findByContactNo(contactNo);
        if (existingUser.isPresent()) {
            userRepository.delete(existingUser.get());
            return true;
        } else {
            return false;
        }
    }

    public String authenticate(String contactNo, String firebaseToken) throws Exception {
        FirebaseToken decodedToken = jwtUtil.verifyFirebaseToken(firebaseToken);
        String rishi = jwtUtil.generateToken(contactNo);
        return rishi;
    }

    public void deleteUser(User user){
        userRepository.delete(user);
    }
}
