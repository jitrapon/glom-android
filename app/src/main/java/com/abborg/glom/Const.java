package com.abborg.glom;

public class Const {

//    public static final String HOST_ADDRESS = "http://putsreq.com/KJVQCdWbHg0xXf2aHWf1";
    public static String HOST_ADDRESS = BuildConfig.SERVER_URL;
    public static String AWS_IDENTIY_POOL_ID = BuildConfig.AWS_COGNITO_IDENTITY_POOL;
    public static String AWS_S3_BUCKET = BuildConfig.AWS_S3_BUCKET;
    public static final String SENT_TOKEN_TO_SERVER = "sentTokenToServer";
    public static final String REGISTRATION_COMPLETE = "registrationComplete";
    public static final String SERVER_URL = BuildConfig.SERVER_URL;

    /************************
     * TABS
     ************************/
    public static final String TAB_CIRCLE = "Circle";
    public static final String TAB_MAP = "Map";
    public static final String TAB_BOARD = "Board";
    public static final String TAB_DISCOVER = "Discover";

    /************************
     * TEST INFO
//     ************************/
//    public static final String TEST_USER_ID = "yoshi3003";
    public static final String TEST_USER_ID = "fatcat18";
    public static final String TEST_CIRCLE_ID = "dev-circle-1";
    public static final String TEST_API_AUTHORIZATION_HEADER = "GLOM-AUTH-TOKEN abcdefghijklmnopqrstuvwxyz0123456789";

    /************************
     * NOTIFICATIONS ID
     ************************/
    public static final int NOTIFY_LOCATION_UPDATE = 0;
    public static final int NOTIFY_BROADCAST_LOCATION = 1;

    /************************
     * ACTIVITY RESULTCODES
     ************************/
    public static final int CREATE_EVENT_RESULT_CODE = 700;
    public static final int UPDATE_EVENT_RESULT_CODE = 701;
    public static final int IMAGE_SELECTED_RESULT_CODE = 702;
    public static final int DRAW_RESULT_CODE = 703;
    public static final int PLACE_AUTOCOMPLETE_REQUEST_CODE = 704;
    public static final int PLACE_PICKER_REQUEST_CODE = 705;
    public static final int OPEN_LINK_RESULT_CODE = 706;

    /************************
     * API
     ************************/
    public static final String API_AUTHORIZATION_HEADER = "Authorization";
    public static final String API_CHECKIN = "/checkin";
    public static final String API_LOCATION = "/location";
    public static final String API_GET_USERS = "/circle/%s/users";
    public static final String API_BOARD = "/circle/%s/board";
    public static final String API_BOARD_ITEM = "/circle/%1$s/board/%2$s";
    public static final String API_GET_MOVIES = "/movie";
    public static final String API_SERVER_STATUS = "/";

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
    public static final String JSON_SERVER_FILE_NAME = "file_name";
    public static final String JSON_SERVER_FILE_SIZE = "size";
    public static final String JSON_SERVER_FILE_MIMETYPE = "mimetype";
    public static final String JSON_SERVER_FILE_NOTE = "note";
    public static final String JSON_SERVER_FILE_PROVIDER = "provider";
    public static final String JSON_SERVER_DRAWING_NAME = "drawing_name";
    public static final String JSON_SERVER_LINK_URL = "url";
    public static final String JSON_SERVER_LINK_THUMBNAIL = "thumbnail";
    public static final String JSON_SERVER_LINK_TITLE = "title";
    public static final String JSON_SERVER_LINK_DESCRIPTION = "description";
    public static final String JSON_SERVER_LINK_MAX_FETCH_PAGES = "max_fetch_pages";
    public static final String JSON_SERVER_LINK_MAX_LINK_DEPTH = "max_link_depth";

    /**
     * HANDLER MESSAGE ID
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
    public static final int MSG_FILE_POST_FAILED = 5016;
    public static final int MSG_FILE_POST_SUCCESS = 5017;
    public static final int MSG_FILE_POST_IN_PROGRESS = 5018;
    public static final int MSG_DOWNLOAD_ITEM = 5019;
    public static final int MSG_FILE_DOWNLOAD_FAILED = 5020;
    public static final int MSG_FILE_DOWNLOAD_COMPLETE = 5021;
    public static final int MSG_FILE_DOWNLOAD_IN_PROGRESS = 5022;
    public static final int MSG_SHOW_TOAST = 5023;
    public static final int MSG_SOCKET_CONNECTED = 5024;
    public static final int MSG_SOCKET_DISCONNECTED = 5025;
    public static final int MSG_SOCKET_DATA_RECEIVED = 5026;
    public static final int MSG_DRAWING_POSTED = 5027;
    public static final int MSG_DRAWING_UPDATED = 5028;
    public static final int MSG_DRAWING_POST_FAILED = 5029;
    public static final int MSG_DRAWING_POST_IN_PROGRESS = 5030;
    public static final int MSG_DRAWING_POST_SUCCESS = 5031;
    public static final int MSG_START_ACTION_MODE = 5032;
    public static final int MSG_SELECT_BOARD_ITEM = 5034;
    public static final int MSG_DRAWING_DOWNLOAD_FAILED = 5035;
    public static final int MSG_DRAWING_DOWNLOAD_COMPLETE = 5036;
    public static final int MSG_DOWNLOAD_DRAWING = 5037;
    public static final int MSG_SERVER_DISCONNECTED = 5038;
    public static final int MSG_SERVER_CONNECTING = 5039;
    public static final int MSG_SERVER_CONNECTED = 5040;
    public static final int MSG_OPEN_LINK = 5041;
    public static final int MSG_GET_CIRCLE = 5042;
    public static final int MSG_LINK_CREATED = 5043;
    public static final int MSG_EDIT_LINK = 5044;
    public static final int MSG_COPY_LINK = 5055;
    public static final int MSG_LINK_UPDATED = 5056;
    public static final int MSG_LINK_CREATED_SUCCESS = 5057;
    public static final int MSG_LINK_CREATED_FAILED = 5058;
    public static final int MSG_LINK_UPDATED_SUCCESS = 5059;
    public static final int MSG_LINK_UPDATED_FAILED = 5060;

    /************************
     * Mimetypes
     ************************/
    public static final String FILE_TYPE_JPEG = "image/jpeg";
    public static final String FILE_TYPE_JPG = "image/jpg";
    public static final String FILE_TYPE_GIF = "image/gif";
    public static final String FILE_TYPE_PNG = "image/png";
    public static final String FILE_TYPE_BMP = "image/x-ms-bmp";
    public static final String FILE_TYPE_WBMP = "image/vnd.wap.wbmp";
    public static final String FILE_TYPE_WEBP = "image/webp";

    /************************
     * WEB RELATED
     ************************/
    public static final String ASSETS_FOLDER = "file:///android_asset/";
    public static final String SERVER_APP_URL = SERVER_URL + "/app";
}
