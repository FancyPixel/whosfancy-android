package it.fancypixel.whosfancy;

/**
 * Created with IntelliJ IDEA.
 * User: michele
 * Date: 10/07/14
 * Time: 10:39
 */
public class Globals {

    private static final String TAG = Globals.class.toString();

    /*
     * Global configuration
     */
    public static final String WHOS_FANCY_REGION = BuildConfig.REGION;
    public static final String PROXIMITY_UUID = BuildConfig.PROXIMITY_UUID;
    public static final Integer MAJOR = BuildConfig.MAJOR;
    public static final Integer MINOR = BuildConfig.MINOR;

    public static final String PREFERENCE_SERVICE_STARTED = "service_stared";
    public static final String PREFERENCE_EMAIL = "email";
    public static final String PREFERENCE_PASSWORD = "password";

    /*
     * URL configuration
     */

    // URL of Web Application
    public static final String URL_BASE = BuildConfig.URL_BASE;

    public static final String URL_PREFIX_CHECKIN = URL_BASE + "/checkin";

    public static final String URL_PREFIX_CHECKOUT = URL_BASE + "/checkout";

}
