package sustainico_backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;
import sustainico_backend.Models.AuthenticationRequest;
import sustainico_backend.Models.AuthenticationResponse;
import sustainico_backend.security.FirebaseTokenVerifier;
import sustainico_backend.util.JwtUtil;

@RestController
@CrossOrigin(origins = "*")
public class AuthenticationController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtTokenUtil;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private FirebaseTokenVerifier firebaseTokenVerifier;

    @RequestMapping(value = "/authenticate", method = RequestMethod.POST)
    public ResponseEntity<?> createAuthenticationToken(@RequestBody AuthenticationRequest authenticationRequest) throws Exception {

        // Verify Firebase token
        String firebaseToken = authenticationRequest.getFirebaseToken();
        String uid = firebaseTokenVerifier.verifyToken(firebaseToken);
        if (uid == null) {
            throw new Exception("Invalid Firebase token");
        }
        // Authenticate user
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(uid, "")
            );
        } catch (BadCredentialsException e) {
            throw new Exception("Incorrect username or password", e);
        }

        final UserDetails userDetails = userDetailsService.loadUserByUsername(uid);
        final String jwt = jwtTokenUtil.generateToken(userDetails.getUsername());
        final String refreshToken = jwtTokenUtil.generateRefreshToken(userDetails.getUsername());

        return ResponseEntity.ok(new AuthenticationResponse(jwt));
    }

    @RequestMapping(value = "/refresh-token", method = RequestMethod.POST)
    public ResponseEntity<?> refreshAuthenticationToken(@RequestBody String refreshToken) throws Exception {
        String contactNo = jwtTokenUtil.extractContactNo(refreshToken);

        if (contactNo != null && jwtTokenUtil.validateToken(refreshToken, contactNo)) {
            final String jwt = jwtTokenUtil.generateToken(contactNo);
            return ResponseEntity.ok(new AuthenticationResponse(jwt));
        } else {
            throw new Exception("Invalid Refresh Token");
        }
    }
}
