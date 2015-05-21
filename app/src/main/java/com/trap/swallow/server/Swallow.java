package com.trap.swallow.server;

/*
 * リクエストメソッドの引数や、レスポンスクラスのフィールドはすべてAPIアクションのparametersおよびresultsに対応しています。
 * 下記のページ(APIアクション一覧)を参考にするとわかりやすい……かもしれません。
 * https://gist.github.com/kazsw/5b425e2b1c62b32bdcc0
 */
public interface Swallow {
	/*
	 * ユーザを探す
	 */
	public User[] findUser(Integer startIndex, Integer endIndex, Long fromTime,
                           Long toTime, Integer[] userIDs, String userNamePattern,
                           String profilePattern, Boolean hasImage);

	/*
	 * ユーザ情報を編集
	 */
	public User modifyUser(String userName, String profile,
                           Integer imageFileID, String password, String email, String web,
                           String twitter, String gcm, String apns, Integer[] observeTagIDs,
                           Boolean observeMention);

	/*
	 * 投稿を取得
	 */
	public Message[] findMessage(Integer startIndex, Integer endIndex,
                                 Long fromTime, Long toTime, Integer[] postIDs,
                                 Integer[] postedUserIDs, Integer[] tagIDs, Integer[] replyPostIDs,
                                 Integer[] destUserIDs, String messagePattern,
                                 Boolean hasAttachment, Boolean isEnquete, Boolean convertToKana);

	/*
	 * 投稿する
	 */
	public Message createMessage(String message, String[] attributes,
                                 Integer[] fileIDs, Integer[] replyPostIDs, Integer[] tagIDs,
                                 Integer[] destUserIDs, String[] enquetes, Integer overwritePostID);

	/*
	 * ファイルを探す
	 */
	public File[] findFile(Integer startIndex, Integer endIndex, Long fromTime,
                           Long toTime, Integer[] fileIDs, Integer[] tagIDs,
                           String fileNamePattern, String fileTypePattern);

	/*
	 * ファイルを取得
	 */
	public byte[] getFile(Integer fileID);

	/*
	 * サムネイルを取得
	 */
	public byte[] getThumbnail(Integer fileID, Integer width, Integer height);

	/*
	 * ファイルを投稿
	 */
	public File createFile(String fileName, String fileType, Integer[] tagIDs,
                           Integer[] folderContent, Integer overwriteFileID, byte[] fileData);

	/*
	 * タグを探す
	 */
	public Tag[] findTag(Integer startIndex, Integer endIndex, Long fromTime,
                         Long toTime, Integer[] tagIDs, Integer minPostNum,
                         Integer maxPostNum, String tagNamePattern);

	/*
	 * タグを作成
	 */
	public Tag createTag(String tagName);

	/*
	 * ふぁぼる
	 */
	public Favorite createFavorite(Integer postID, Integer favNum);

	/*
	 * アンケートに回答する
	 */
	public Answer createAnswer(Integer postID, Integer answerIndex);

	/*
	 * 既読をつける
	 */
	public Received createReceived(Integer postID);

	/*
	 * レスポンス: ユーザ情報
	 */
	class User {
		private Integer UserID;
		private Long Joined;
		private String UserName;
		private String Profile;
		private Integer Image;

		public User(Integer userID, Long joined, String userName,
				String profile, Integer image) {
			UserID = userID;
			Joined = joined;
			UserName = userName;
			Profile = profile;
			Image = image;
		}

		public Integer getUserID() {
			return UserID;
		}

		public Long getJoined() {
			return Joined;
		}

		public String getUserName() {
			return UserName;
		}

		public String getProfile() {
			return Profile;
		}

		public Integer getImage() {
			return Image;
		}
	}

	/*
	 * レスポンス: ユーザ詳細情報
	 */
	class UserDetail extends User {
		private String Email;
		private String Web;
		private String Twitter;
		private String GCM;
		private String APNs;
		private Integer[] ObserveTag;
		private Boolean ObserveMention;

		public UserDetail(Integer userID, Long joined, String userName,
				String profile, Integer image, String email, String web,
				String twitter, String gcm, String apns, Integer[] observeTag,
				Boolean observeMention) {
			super(userID, joined, userName, profile, image);
			Email = email;
			Web = web;
			Twitter = twitter;
			GCM = gcm;
			APNs = apns;
			ObserveTag = observeTag;
			ObserveMention = observeMention;
		}

		public String getEmail() {
			return Email;
		}

		public String getWeb() {
			return Web;
		}

		public String getTwitter() {
			return Twitter;
		}

		public String getGCM() {
			return GCM;
		}

		public String getAPNs() {
			return APNs;
		}

		public Integer[] getObserveTag() {
			return ObserveTag;
		}

		public Boolean getObserveMention() {
			return ObserveMention;
		}
	}

	/*
	 * レスポンス: 投稿
	 */
	class Message {
		private Integer PostID;
		private Long Posted;
		private Integer UserID;
		private String Message;
		private String[] Attribute;
		private Integer[] FileID;
		private Integer[] TagID;
		private Integer[] Reply;
		private Integer[] Dest;
		private String[] Enquete;
		private Integer FavCount;
		private Favorite[] Fav;
		private Integer AnswerCount;
		private Answer[] Answer;
		private Integer ReceivedCount;
		private Received[] Received;

		public Message(Integer postID, Long posted, Integer userID,
				String message, String[] attribute, Integer[] fileID,
				Integer[] tagID, Integer[] reply, Integer[] dest,
				String[] enquete, Integer favCount, Favorite[] fav,
				Integer answerCount, Answer[] answer, Integer receivedCount,
				Received[] received) {
			PostID = postID;
			Posted = posted;
			UserID = userID;
			Message = message;
			Attribute = attribute;
			FileID = fileID;
			TagID = tagID;
			Reply = reply;
			Dest = dest;
			Enquete = enquete;
			FavCount = favCount;
			Fav = fav;
			AnswerCount = answerCount;
			Answer = answer;
			ReceivedCount = receivedCount;
			Received = received;
		}

		public Integer getPostID() {
			return PostID;
		}

		public Long getPosted() {
			return Posted;
		}

		public Integer getUserID() {
			return UserID;
		}

		public String getMessage() {
			return Message;
		}

		public String[] getAttribute() {
			return Attribute;
		}

		public Integer[] getFileID() {
			return FileID;
		}

		public Integer[] getTagID() {
			return TagID;
		}

		public Integer[] getReply() {
			return Reply;
		}

		public Integer[] getDest() {
			return Dest;
		}

		public String[] getEnquete() {
			return Enquete;
		}

		public Integer getFavCount() {
			return FavCount;
		}

		public Favorite[] getFav() {
			return Fav;
		}

		public Integer getAnswerCount() {
			return AnswerCount;
		}

		public Answer[] getAnswer() {
			return Answer;
		}

		public Integer getReceivedCount() {
			return ReceivedCount;
		}

		public Received[] getReceived() {
			return Received;
		}
	}

	/*
	 * レスポンス: ファイル
	 */
	class File {
		private Integer FileID;
		private Long Created;
		private String FileName;
		private String FileType;
		private Integer[] TagID;
		private Integer[] FolderContent;

		public File(Integer fileID, Long created, String fileName,
				String fileType, Integer[] tagID, Integer[] folderContent) {
			FileID = fileID;
			Created = created;
			FileName = fileName;
			FileType = fileType;
			TagID = tagID;
			FolderContent = folderContent;
		}

		public Integer getFileID() {
			return FileID;
		}

		public Long getCreated() {
			return Created;
		}

		public String getFileName() {
			return FileName;
		}

		public String getFileType() {
			return FileType;
		}

		public Integer[] getTagID() {
			return TagID;
		}

		public Integer[] getFolderContent() {
			return FolderContent;
		}
	}

	/*
	 * レスポンス: タグ
	 */
	class Tag {
		private Integer TagID;
		private Long Updated;
		private String TagName;
		private Integer PostNum;

		public Tag(Integer tagID, Long updated, String tagName, Integer postNum) {
			TagID = tagID;
			Updated = updated;
			TagName = tagName;
			PostNum = postNum;
		}

		public Integer getTagID() {
			return TagID;
		}

		public Long getUpdated() {
			return Updated;
		}

		public String getTagName() {
			return TagName;
		}

		public Integer getPostNum() {
			return PostNum;
		}
	}

	/*
	 * レスポンス: お気に入り
	 */
	class Favorite {
		private Integer UserID;
		private Integer PostID;
		private Long Updated;
		private Integer FavNum;

		public Favorite(Integer userID, Integer postID, Long updated,
				Integer favNum) {
			UserID = userID;
			PostID = postID;
			Updated = updated;
			FavNum = favNum;
		}

		public Integer getUserID() {
			return UserID;
		}

		public Integer getPostID() {
			return PostID;
		}

		public Long getUpdated() {
			return Updated;
		}

		public Integer getFavNum() {
			return FavNum;
		}
	}

	/*
	 * レスポンス: アンケート回答
	 */
	class Answer {
		private Integer UserID;
		private Integer PostID;
		private Long Updated;
		private Integer Answer;

		public Answer(Integer userID, Integer postID, Long updated,
				Integer answer) {
			UserID = userID;
			PostID = postID;
			Updated = updated;
			Answer = answer;
		}

		public Integer getUserID() {
			return UserID;
		}

		public Integer getPostID() {
			return PostID;
		}

		public Long getUpdated() {
			return Updated;
		}

		public Integer getAnswer() {
			return Answer;
		}
	}

	/*
	 * レスポンス: 既読
	 */
	class Received {
		private Integer UserID;
		private Integer PostID;
		private Long Updated;

		public Received(Integer userID, Integer postID, Long updated) {
			UserID = userID;
			PostID = postID;
			Updated = updated;
		}

		public Integer getUserID() {
			return UserID;
		}

		public Integer getPostID() {
			return PostID;
		}

		public Long getUpdated() {
			return Updated;
		}
	}
}