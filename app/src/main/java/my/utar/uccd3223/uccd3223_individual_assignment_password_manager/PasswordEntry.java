package my.utar.uccd3223.uccd3223_individual_assignment_password_manager;

public class PasswordEntry {
    private int id;
    private String title;
    private String url;
    private String username;
    private String password;
    private String category;
    private String extraPin;
    private String remark;

    public PasswordEntry() {}

    public PasswordEntry(int id, String title, String url, String username,
                         String password, String category, String extraPin,
                         String remark) {
        this.id = id;
        this.title = title;
        this.url = url;
        this.username = username;
        this.password = password;
        this.category = category;
        this.extraPin = extraPin;
        this.remark = remark;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getExtraPin() { return extraPin; }
    public void setExtraPin(String extraPin) { this.extraPin = extraPin; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
