package com.trap.swallow.server;

import java.io.StringWriter;

public class SwallowClient implements Swallow {
	private SwallowSecurity security;

	public SwallowClient(SwallowSecurity security) {
		this.security = security;
	}

	@Override
	public User[] findUser(Integer startIndex, Integer endIndex, Long fromTime,
						   Long toTime, Integer[] userIDs, String userNamePattern,
						   String profilePattern, Boolean hasImage) throws SwallowException {
		try {
			StringWriter stringWriter = new StringWriter();
			RequestJsonWriter jsonWriter = new RequestJsonWriter(stringWriter);

			jsonWriter.beginObject()
					.name("action").value("FIND_USER")
					.name("parameters").beginObject()
					.name("index").beginObject()
					.name("from").value(startIndex)
					.name("to").value(endIndex)
					.endObject()
					.name("datetime").beginObject()
					.name("from").value(fromTime)
					.name("to").value(toTime)
					.endObject()
					.name("id").value(userIDs)
					.name("userName").value(userNamePattern)
					.name("profile").value(profilePattern)
					.name("hasImage").value(hasImage)
					.endObject()
					.endObject();

			jsonWriter.close();
			return security.simpleRequest(stringWriter.toString(), null, User[].class);
		} catch (Exception e) {
			throw new SwallowException("Internal error", null, e);
		}
	}

	@Override
	public UserDetail modifyUser(String userName, String profile,
								 Integer imageFileID, String password, String email,
								 Integer[] observeTagIDs, String gcm) throws SwallowException {
		try {
			StringWriter stringWriter = new StringWriter();
			RequestJsonWriter jsonWriter = new RequestJsonWriter(stringWriter);

			jsonWriter.beginObject()
					.name("action").value("MODIFY_USER")
					.name("parameters").beginObject()
					.name("userName").value(userName)
					.name("profile").value(profile)
					.name("image").value(imageFileID)
					.name("password").value(password)
					.name("email").value(email)
					.name("observe").value(observeTagIDs)
					.name("gcm").value(gcm)
					.endObject()
					.endObject();

			jsonWriter.close();
			return security.simpleRequest(stringWriter.toString(), null, UserDetail.class);
		} catch (Exception e) {
			throw new SwallowException("Internal error", null, e);
		}
	}

	@Override
	public Message[] findMessage(Integer startIndex, Integer endIndex,
								 Long fromTime, Long toTime, Integer[] postIDs,
								 Integer[] postedUserIDs, Integer[] tagIDs, Integer[] replyPostIDs,
								 String messagePattern, Boolean hasAttachment, Boolean isEnquete,
								 Boolean convertToKana) throws SwallowException {
		try {
			StringWriter stringWriter = new StringWriter();
			RequestJsonWriter jsonWriter = new RequestJsonWriter(stringWriter);

			jsonWriter.beginObject()
					.name("action").value("FIND_MESSAGE")
					.name("parameters").beginObject()
					.name("index").beginObject()
					.name("from").value(startIndex)
					.name("to").value(endIndex)
					.endObject()
					.name("datetime").beginObject()
					.name("from").value(fromTime)
					.name("to").value(toTime)
					.endObject()
					.name("id").value(postIDs)
					.name("user").value(postedUserIDs)
					.name("tag").value(tagIDs)
					.name("reply").value(replyPostIDs)
					.name("message").value(messagePattern)
					.name("hasAttachment").value(hasAttachment)
					.name("isEnquete").value(isEnquete)
					.name("convertToKana").value(convertToKana)
					.endObject()
					.endObject();

			jsonWriter.close();
			return security.simpleRequest(stringWriter.toString(), null, Message[].class);
		} catch (Exception e) {
			throw new SwallowException("Internal error", null, e);
		}
	}

	@Override
	public Message createMessage(String message, Integer[] fileIDs,
								 Integer[] tagIDs, Integer[] replyPostIDs, String[] enquetes,
								 Integer overwritePostID) throws SwallowException {
		try {
			StringWriter stringWriter = new StringWriter();
			RequestJsonWriter jsonWriter = new RequestJsonWriter(stringWriter);

			jsonWriter.beginObject()
					.name("action").value("CREATE_MESSAGE")
					.name("parameters").beginObject()
					.name("message").value(message)
					.name("fileID").value(fileIDs)
					.name("tag").value(tagIDs)
					.name("reply").value(replyPostIDs)
					.name("enquete").value(enquetes)
					.name("overwrite").value(overwritePostID)
					.endObject()
					.endObject();

			jsonWriter.close();
			return security.simpleRequest(stringWriter.toString(), null, Message.class);
		} catch (Exception e) {
			throw new SwallowException("Internal error", null, e);
		}
	}

	@Override
	public File[] findFile(Integer startIndex, Integer endIndex, Long fromTime,
						   Long toTime, Integer[] fileIDs, Integer[] tagIDs,
						   String fileNamePattern, String fileTypePattern)
			throws SwallowException {
		try {
			StringWriter stringWriter = new StringWriter();
			RequestJsonWriter jsonWriter = new RequestJsonWriter(stringWriter);

			jsonWriter.beginObject()
					.name("action").value("FIND_FILE")
					.name("parameters").beginObject()
					.name("index").beginObject()
					.name("from").value(startIndex)
					.name("to").value(endIndex)
					.endObject()
					.name("datetime").beginObject()
					.name("from").value(fromTime)
					.name("to").value(toTime)
					.endObject()
					.name("id").value(fileIDs)
					.name("tag").value(tagIDs)
					.name("fileName").value(fileNamePattern)
					.name("fileType").value(fileTypePattern)
					.endObject()
					.endObject();

			jsonWriter.close();
			return security.simpleRequest(stringWriter.toString(), null, File[].class);
		} catch (Exception e) {
			throw new SwallowException("Internal error", null, e);
		}
	}

	@Override
	public byte[] getFile(Integer fileID) throws SwallowException {
		try {
			StringWriter stringWriter = new StringWriter();
			RequestJsonWriter jsonWriter = new RequestJsonWriter(stringWriter);

			jsonWriter.beginObject()
					.name("action").value("GET_FILE")
					.name("parameters").beginObject()
					.name("fileID").value(fileID)
					.endObject()
					.endObject();

			jsonWriter.close();
			return security.fileRequest(stringWriter.toString());
		} catch (Exception e) {
			throw new SwallowException("Internal error", null, e);
		}
	}

	@Override
	public byte[] getThumbnail(Integer fileID, Integer width, Integer height)
			throws SwallowException {
		try {
			StringWriter stringWriter = new StringWriter();
			RequestJsonWriter jsonWriter = new RequestJsonWriter(stringWriter);

			jsonWriter.beginObject()
					.name("action").value("GET_THUMBNAIL")
					.name("parameters").beginObject()
					.name("fileID").value(fileID)
					.name("width").value(width)
					.name("height").value(height)
					.endObject()
					.endObject();

			jsonWriter.close();
			return security.fileRequest(stringWriter.toString());
		} catch (Exception e) {
			throw new SwallowException("Internal error", null, e);
		}
	}

	@Override
	public File createFile(String fileName, String fileType, Integer[] tagIDs,
						   Integer[] folderContent, Integer overwriteFileID, byte[] fileData)
			throws SwallowException {
		try {
			StringWriter stringWriter = new StringWriter();
			RequestJsonWriter jsonWriter = new RequestJsonWriter(stringWriter);

			jsonWriter.beginObject()
					.name("action").value("CREATE_FILE")
					.name("parameters").beginObject()
					.name("fileName").value(fileName)
					.name("fileType").value(fileType)
					.name("tagID").value(tagIDs)
					.name("folderContents").value(folderContent)
					.name("overwrite").value(overwriteFileID)
					.endObject()
					.endObject();

			jsonWriter.close();
			return security.simpleRequest(stringWriter.toString(), fileData, File.class);
		} catch (Exception e) {
			throw new SwallowException("Internal error", null, e);
		}
	}

	@Override
	public Tag[] findTag(Integer startIndex, Integer endIndex, Long fromTime,
						 Long toTime, Integer[] tagIDs, String tagNamePattern,
						 Integer[] participantUserIDs, Boolean isInvisible,
						 Boolean isArchived) throws SwallowException {
		try {
			StringWriter stringWriter = new StringWriter();
			RequestJsonWriter jsonWriter = new RequestJsonWriter(stringWriter);

			jsonWriter.beginObject()
					.name("action").value("FIND_TAG")
					.name("parameters").beginObject()
					.name("index").beginObject()
					.name("from").value(startIndex)
					.name("to").value(endIndex)
					.endObject()
					.name("datetime").beginObject()
					.name("from").value(fromTime)
					.name("to").value(toTime)
					.endObject()
					.name("id").value(tagIDs)
					.name("tagName").value(tagNamePattern)
					.name("participant").value(participantUserIDs)
					.name("isInvisible").value(isInvisible)
					.name("isArchived").value(isArchived)
					.endObject()
					.endObject();

			jsonWriter.close();
			return security.simpleRequest(stringWriter.toString(), null, Tag[].class);
		} catch (Exception e) {
			throw new SwallowException("Internal error", null, e);
		}
	}

	@Override
	public Tag createTag(String tagName, Integer[] participantUserIDs,
						 Boolean invisible, Integer archiveTagtID) throws SwallowException {
		try {
			StringWriter stringWriter = new StringWriter();
			RequestJsonWriter jsonWriter = new RequestJsonWriter(stringWriter);

			jsonWriter.beginObject()
					.name("action").value("CREATE_TAG")
					.name("parameters").beginObject()
					.name("tagName").value(tagName)
					.name("participant").value(participantUserIDs)
					.name("invisible").value(invisible)
					.name("archive").value(archiveTagtID)
					.endObject()
					.endObject();

			jsonWriter.close();
			return security.simpleRequest(stringWriter.toString(), null, Tag.class);
		} catch (Exception e) {
			throw new SwallowException("Internal error", null, e);
		}
	}

	@Override
	public Favorite createFavorite(Integer postID, Integer favNum)
			throws SwallowException {
		try {
			StringWriter stringWriter = new StringWriter();
			RequestJsonWriter jsonWriter = new RequestJsonWriter(stringWriter);

			jsonWriter.beginObject()
					.name("action").value("CREATE_FAVORITE")
					.name("parameters").beginObject()
					.name("post").value(postID)
					.name("num").value(favNum)
					.endObject()
					.endObject();

			jsonWriter.close();
			return security.simpleRequest(stringWriter.toString(), null, Favorite.class);
		} catch (Exception e) {
			throw new SwallowException("Internal error", null, e);
		}
	}

	@Override
	public Answer createAnswer(Integer postID, Integer answerIndex)
			throws SwallowException {
		try {
			StringWriter stringWriter = new StringWriter();
			RequestJsonWriter jsonWriter = new RequestJsonWriter(stringWriter);

			jsonWriter.beginObject()
					.name("action").value("CREATE_ANSWER")
					.name("parameters").beginObject()
					.name("post").value(postID)
					.name("answer").value(answerIndex)
					.endObject()
					.endObject();

			jsonWriter.close();
			return security.simpleRequest(stringWriter.toString(), null, Answer.class);
		} catch (Exception e) {
			throw new SwallowException("Internal error", null, e);
		}
	}

	@Override
	public Received createReceived(Integer postID) throws SwallowException {
		try {
			StringWriter stringWriter = new StringWriter();
			RequestJsonWriter jsonWriter = new RequestJsonWriter(stringWriter);

			jsonWriter.beginObject()
					.name("action").value("CREATE_RECEIVED")
					.name("parameters").beginObject()
					.name("post").value(postID)
					.endObject()
					.endObject();

			jsonWriter.close();
			return security.simpleRequest(stringWriter.toString(), null, Received.class);
		} catch (Exception e) {
			throw new SwallowException("Internal error", null, e);
		}
	}

}