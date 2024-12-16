package sustainico_backend.Models;

import java.util.List;

public class UserWithHomesDTO {
    private User user;
    private List<Home> homes;

    // Constructors, getters, and setters
    public UserWithHomesDTO(User user, List<Home> homes) {
        this.user = user;
        this.homes = homes;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<Home> getHomes() {
        return homes;
    }

    public void setHomes(List<Home> homes) {
        this.homes = homes;
    }
}
