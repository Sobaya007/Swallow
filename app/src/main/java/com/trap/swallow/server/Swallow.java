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
						   String profilePattern, Boolean hasImage) throws SwallowException;

	/*
	 * ユーザ情報を編集
	 */
	public UserDetail modifyUser(String userName, String profile,
								 Integer imageFileID, String password, String email, String web,
								 String twitter, String gcm, String apns, Integer[] observeTagIDs,
								 Boolean observeMention) throws SwallowException;

	/*
	 * 投稿を取得
	 */
	public Message[] findMessage(Integer startIndex, Integer endIndex,
								 Long fromTime, Long toTime, Integer[] postIDs,
								 Integer[] postedUserIDs, Integer[] tagIDs, Integer[] replyPostIDs,
								 Integer[] destUserIDs, String messagePattern,
								 Boolean hasAttachment, Boolean isEnquete, Boolean convertToKana)
			throws SwallowException;

	/*
	 * 投稿する
	 */
	public Message createMessage(String message, Integer[] fileIDs,
								 Integer[] tagIDs, Integer[] replyPostIDs, Integer[] destUserIDs,
								 String[] enquetes, Integer overwritePostID) throws SwallowException;

	/*
	 * ファイルを探す
	 */
	public File[] findFile(Integer startIndex, Integer endIndex, Long fromTime,
						   Long toTime, Integer[] fileIDs, Integer[] tagIDs,
						   String fileNamePattern, String fileTypePattern)
			throws SwallowException;

	/*
	 * ファイルを取得
	 */
	public byte[] getFile(Integer fileID) throws SwallowException;

	/*
	 * サムネイルを取得
	 */
	public byte[] getThumbnail(Integer fileID, Integer width, Integer height)
			throws SwallowException;

	/*
	 * ファイルを投稿
	 */
	public File createFile(String fileName, String fileType, Integer[] tagIDs,
						   Integer[] folderContent, Integer overwriteFileID, byte[] fileData)
			throws SwallowException;

	/*
	 * タグを探す
	 */
	public Tag[] findTag(Integer startIndex, Integer endIndex, Long fromTime,
						 Long toTime, Integer[] tagIDs, String tagNamePattern,
						 Boolean isInvisible) throws SwallowException;

	/*
	 * タグを作成
	 */
	public Tag createTag(String tagName, Boolean invisible)
			throws SwallowException;

	/*
	 * ふぁぼる
	 */
	public Favorite createFavorite(Integer postID, Integer favNum)
			throws SwallowException;

	/*
	 * アンケートに回答する
	 */
	public Answer createAnswer(Integer postID, Integer answerIndex)
			throws SwallowException;

	/*
	 * 既読をつける
	 */
	public Received createReceived(Integer postID) throws SwallowException;

	/*
	 * レスポンス: ユーザ情報
	 */
	class User {
		private Integer UserID;
		private Long Joined;
		private String UserName;
		private String Profile;
		private Integer Image;

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
		private Boolean Invisible;

		public Integer getTagID() {
			return TagID;
		}

		public Long getUpdated() {
			return Updated;
		}

		public String getTagName() {
			return TagName;
		}

		public Boolean getInvisible() {
			return Invisible;
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