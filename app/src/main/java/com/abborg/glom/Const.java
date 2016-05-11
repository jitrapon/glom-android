package com.abborg.glom;

public class Const {

//    public static final String HOST_ADDRESS = "http://putsreq.com/KJVQCdWbHg0xXf2aHWf1";
    public static String HOST_ADDRESS = BuildConfig.SERVER_URL;
    public static final String SENT_TOKEN_TO_SERVER = "sentTokenToServer";
    public static final String REGISTRATION_COMPLETE = "registrationComplete";

    /**
     * TEST INFO
     * **/
    public static final String TEST_USER_ID = "yoshi3003";
//    public static final String TEST_USER_ID = "fatcat18";
    public static final String TEST_CIRCLE_ID = "dev-test-circle-1";
    public static final String TEST_API_AUTHORIZATION_HEADER = "GLOM-AUTH-TOKEN abcdefghijklmnopqrstuvwxyz0123456789";

    /**
     * NOTIFICATIONS ID
     */
    public static final int NOTIFY_LOCATION_UPDATE = 0;
    public static final int NOTIFY_BROADCAST_LOCATION = 1;

    /**
     * ACTIVITY RESULTCODES
     */
    public static final int CREATE_EVENT_RESULT_CODE = 700;
    public static final int UPDATE_EVENT_RESULT_CODE = 701;
    public static final int IMAGE_SELECTED_RESULT_CODE = 702;

    /**
     * API
     */
    public static final String API_AUTHORIZATION_HEADER = "Authorization";
    public static final String API_CHECKIN = "/checkin";
    public static final String API_LOCATION = "/location";
    public static final String API_GET_USERS = "/circle/%s/users";
    public static final String API_BOARD = "/circle/%s/board";
    public static final String API_BOARD_ITEM = "/circle/%1$s/board/%2$s";
    public static final String API_GET_MOVIES = "/movie";

    public static final String JSON_SERVER_MESSAGE = "message";
    public static final String JSON_SERVER_USERID = "user_id";
    public static final String JSON_SERVER_USERNAME = "user_name";
    public static final String JSON_SERVER_USER_AVATAR = "avatar";
    public static final String JSON_SERVER_USERTYPE = "user_type";
    public static final String JSON_SERVER_USERIDS = "user_ids";
    public static final String JSON_SERVER_CIRCLEID = "circle_id";
    public static final String JSON_SERVER_GCM_TOKEN = "gcm_token";
    public static final String JSON_SERVER_LOCATION = "location";
    public static final String JSON_SERVER_LOCATION_LAT = "lat";
    public static final String JSON_SERVER_LOCATION_LONG = "long";
    public static final String JSON_SERVER_OP = "op";
    public static final String JSON_SERVER_USERS = "users";
    public static final String JSON_SERVER_ITEMS = "items";
    public static final String JSON_SERVER_ITEM_ID = "item_id";
    public static final String JSON_SERVER_ITEM_TYPE = "item_type";
    public static final String JSON_SERVER_CREATED_TIME = "created_time";
    public static final String JSON_SERVER_UPDATED_TIME = "updated_time";
    public static final String JSON_SERVER_INFO = "info";
    public static final String JSON_SERVER_EVENT_NAME = "event_name";
    public static final String JSON_SERVER_EVENT_START_TIME = "start_time";
    public static final String JSON_SERVER_EVENT_END_TIME = "end_time";
    public static final String JSON_SERVER_EVENT_PLACE_ID = "place_id";
    public static final String JSON_SERVER_EVENT_NOTE = "note";
    public static final String JSON_SERVER_ERROR = "error";
    public static final String JSON_SERVER_TIME = "time";
    public static final String JSON_SERVER_MESSAGE_ID = "id";
    public static final String JSON_SERVER_MESSAGE_TYPE = "type";
    public static final String JSON_SERVER_SENDER = "sender";
    public static final String JSON_VALUE_MESSAGE_TYPE_TEXT = "text";
    public static final String JSON_SERVER_RATINGS = "ratings";
    public static final String JSON_SERVER_RATING_SCORE = "score";
    public static final String JSON_SERVER_RATING_SOURCE = "source";
    public static final String JSON_SERVER_IMAGES = "images";
    public static final String JSON_SERVER_IMAGE_TYPE = "type";
    public static final String JSON_SERVER_IMAGE_THUMBNAIL = "thumbnail";
    public static final String JSON_SERVER_IMAGE_URL = "url";
    public static final String JSON_SERVER_VIDEOS = "videos";
    public static final String JSON_SERVER_VIDEO_TYPE = "type";
    public static final String JSON_SERVER_VIDEO_LANG = "lang";
    public static final String JSON_SERVER_VIDEO_SOURCE = "source";
    public static final String JSON_SERVER_VIDEO_URL = "url";
    public static final String JSON_SERVER_FEEDS = "feeds";
    public static final String JSON_SERVER_FEED_NAME = "name";
    public static final String JSON_SERVER_FEED_SOURCE = "url";
    public static final String JSON_SERVER_TITLE = "title";
    public static final String JSON_SERVER_SUMMARY = "summary";
    public static final String JSON_SERVER_GENRE = "genre";
    public static final String JSON_SERVER_LANG = "language";
    public static final String JSON_SERVER_RELEASE_DATE = "release_date";
    public static final String JSON_SERVER_DIRECTOR = "director";
    public static final String JSON_SERVER_CAST = "cast";

    /**
     * HANDLER WHAT CONSTANTS
     */
    public static final int MSG_GET_USERS = 5000;
    public static final int MSG_ITEM_TO_DELETE = 5001;
    public static final int MSG_GET_ITEMS = 5002;
    public static final int MSG_EVENT_CREATED = 5003;
    public static final int MSG_EVENT_CREATED_SUCCESS = 5004;
    public static final int MSG_EVENT_CREATED_FAILED = 5005;
    public static final int MSG_EVENT_UPDATED = 5006;
    public static final int MSG_EVENT_UPDATED_SUCCESS = 5007;
    public static final int MSG_EVENT_UPDATED_FAILED = 5008;
    public static final int MSG_ITEM_DELETED_SUCCESS = 5010;
    public static final int MSG_ITEM_DELETED_FAILED = 5011;
    public static final int MSG_INIT_SUCCESS = 5012;
    public static final int MSG_DISCOVER_ITEM = 5013;
    public static final int MSG_PLAY_YOUTUBE_VIDEO = 5014;
    public static final int MSG_FILE_POSTED = 5015;
    public static final int MSG_CHANGE_SYNC_STATUS = 5016;

    /**
     * Mimetypes
     */
    public static final String FILE_TYPE_JPEG = "image/jpeg";
    public static final String FILE_TYPE_JPG = "image/jpg";
    public static final String FILE_TYPE_GIF = "image/gif";
    public static final String FILE_TYPE_PNG = "image/png";
    public static final String FILE_TYPE_BMP = "image/x-ms-bmp";
    public static final String FILE_TYPE_WBMP = "image/vnd.wap.wbmp";
    public static final String FILE_TYPE_WEBP = "image/webp";
}
