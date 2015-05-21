package com.trap.swallow.talk;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.text.Spannable;
import android.text.style.UnderlineSpan;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.trap.swallow.server.SCM;
import com.trap.swallow.server.Swallow;
import com.trap.swallow.server.Swallow.Message;
import com.trap.swallow.server.Swallow.User;
import com.trap.swallow.swallow.R;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;

public class MessageView extends LinearLayout{

    private static final int[] tagColors = {
            Color.rgb(230, 0, 18), Color.rgb(0, 160, 233), Color.rgb(0, 153, 68), Color.rgb(14, 110, 184), Color.rgb(96,25, 134)
    };

    public final LayoutParams lp;
    public final Message mInfo;

    public int arrivalY = 0;
    public float t;

    public int answerIndex;

    public MessageView(final TalkActivity context, final Message mInfo, final TalkManager tvManager) throws IOException {
        super(context);

        this.mInfo = mInfo;

        View v = context.getLayoutInflater().inflate(R.layout.message_view, this);

        //メッセージ
        {
            TextView messageView = (TextView)v.findViewById(R.id.message_text);
            messageView.setText(mInfo.getMessage());
        }

        //リプ
        {
            LinearLayout replyLayout = (LinearLayout)v.findViewById(R.id.reply_text_layout);
            Integer[] reply = mInfo.getReply();
            if (reply != null && reply.length != 0) {
                Calendar c = Calendar.getInstance();
                for (int postId : reply) {
                    StringBuilder sb = new StringBuilder();
                    final MessageView mv = tvManager.findMessageViewById(postId);
                    Message message = mv.mInfo;
                    sb.append(">>");
                    sb.append(tvManager.findUserById(message.getUserID()).getUserName());
                    c.setTimeInMillis(message.getPosted());
                    int h = c.get(Calendar.HOUR_OF_DAY);
                    int m = c.get(Calendar.MINUTE);
                    sb.append(" ");
                    sb.append((h < 10 ? "0" : "") + h + ":" + (m < 10 ? "0" : "") + m);
                    TextView replyView = new TextView(context);
                    replyView.setLayoutParams(MyUtils.getLayoutparams(MyUtils.MP, MyUtils.WC));
                    replyView.setText(sb.toString());
                    replyView.setTextColor(Color.rgb(144, 144, 144));
                    replyLayout.addView(replyView);
                    replyView.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            context.scrollView.scrollTo(0, (int) mv.getY());
                        }
                    });
                }
            }
        }

        User sender = tvManager.findUserById(mInfo.getUserID());
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        final SharedPreferences.Editor editor = sp.edit();
        //アイコン
        {
            ImageView iconView = (ImageView)v.findViewById(R.id.icon_image);
            Integer iconID = sender.getImage();
            if (iconID != null) {
                InputStream in = new ByteArrayInputStream(MyUtils.getFileByteArray(iconID, sp));
                Bitmap bmp = BitmapFactory.decodeStream(in);
                iconView.setImageBitmap(bmp);
            }
            iconView.setOnClickListener(new OnClickListener() {
                public void onClick(View paramView) {

                }
            });
        }

        //名前
        {
            TextView nameView = (TextView)v.findViewById(R.id.name_text);
            String name = sender.getUserName();
            Spannable t = Spannable.Factory.getInstance().newSpannable(name);
            UnderlineSpan us = new UnderlineSpan();
            t.setSpan(us, 0, name.length(), t.getSpanFlags(us));
            nameView.setText(name);
        }

        //時間
        {
            TextView timeView = (TextView)v.findViewById(R.id.time_text);
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(mInfo.getPosted());
            int h = c.get(Calendar.HOUR_OF_DAY);
            int m = c.get(Calendar.MINUTE);
            timeView.setText((h < 10 ? "0" : "") + h + ":" + (m < 10 ? "0" : "") + m);
        }

        //ファイル
        {
            Integer[] fileIdArray = mInfo.getFileID();
            if (fileIdArray != null) {
                LinearLayout fileLayout = (LinearLayout)v.findViewById(R.id.file_layout);
                for (final int id : fileIdArray) {
                    final Swallow.File file = tvManager.findFileById(id);
                    LinearLayout layout = new LinearLayout(context);
                    layout.setOrientation(LinearLayout.VERTICAL);
                    layout.setLayoutParams(MyUtils.getLayoutparams(MyUtils.WC, MyUtils.WC));
                    layout.setGravity(Gravity.CENTER_HORIZONTAL);
                    layout.setPadding(5, 0, 5, 0);
                    layout.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            String fileType = file.getFileType();
                            Intent intent = new Intent();
                            intent.setType(fileType);
                            if (fileType.equals("application/pdf")) {
                                intent.setAction(Intent.ACTION_VIEW);
                                try {
                                    FileOutputStream fos = new FileOutputStream(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), file.getFileName()));
                                    fos.write(MyUtils.getFileByteArray(file.getFileID(), sp));
                                    fos.flush();
                                    fos.close();
                                } catch (FileNotFoundException e) {
                                    // TODO 自動生成された catch ブロック
                                    e.printStackTrace();
                                } catch (IOException e) {
                                    // TODO 自動生成された catch ブロック
                                    e.printStackTrace();
                                }
                                String path = "file://" + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + file.getFileName();
                                MyUtils.showShortToast(context, path);
                                intent.setDataAndType(Uri.parse(path), "application/pdf");
                                context.startActivity(intent);
                            } else if (fileType.equals("text/plain")) {
                                intent.setAction(Intent.ACTION_SEND);
                                String text = new String(MyUtils.getFileByteArray(file.getFileID(), sp));
                                intent.putExtra(Intent.EXTRA_TEXT, text);
                                context.startActivity(intent);
                            } else if (fileType.startsWith("image")) {
                                intent.setAction(Intent.ACTION_VIEW);
                                try {
                                    FileOutputStream fos = new FileOutputStream(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), file.getFileName()));
                                    fos.write(MyUtils.getFileByteArray(file.getFileID(), sp));
                                    fos.flush();
                                    fos.close();
                                } catch (FileNotFoundException e) {
                                    // TODO 自動生成された catch ブロック
                                    e.printStackTrace();
                                } catch (IOException e) {
                                    // TODO 自動生成された catch ブロック
                                    e.printStackTrace();
                                }
                                String path = "file://" + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + file.getFileName();
                                intent.setDataAndType(Uri.parse(path), file.getFileType());
                                context.startActivity(intent);
                            }
                        }
                    });
                    ImageView iv = new ImageView(context);
                    iv.setLayoutParams(MyUtils.getLayoutparams(100, 100));
                    String fileType = file.getFileType();
                    if (fileType.startsWith("image")) {
                        iv.setImageBitmap(BitmapFactory.decodeStream(new ByteArrayInputStream(MyUtils.getThumbnailByteArray(id, 100, 100, sp))));
                    } else {
                        iv.setImageBitmap(MyUtils.getImageFromPath(getResources(), fileType));
                    }
                    layout.addView(iv);
                    TextView tv = new TextView(context);
                    tv.setText(file.getFileName());
                    tv.setTextSize(10f);
                    tv.setLayoutParams(MyUtils.getLayoutparams(MyUtils.WC, MyUtils.WC));
                    tv.setGravity(Gravity.CENTER_HORIZONTAL);
                    layout.addView(tv);
                    fileLayout.addView(layout);
                }
            }
        }

        //アンケート
        {
            final String[] enqeteList = mInfo.getEnquete();
            if (enqeteList != null){
                LinearLayout enqueteLayout = (LinearLayout)v.findViewById(R.id.enquete_layout);
                for (String selection : enqeteList) {
                    LinearLayout layout = new LinearLayout(context);
                    layout.setOrientation(LinearLayout.HORIZONTAL);
                    layout.setLayoutParams(
                            new LayoutParams(
                                    LayoutParams.MATCH_PARENT,
                                    LayoutParams.WRAP_CONTENT));

                    ImageView im = new ImageView(context);
                    im.setLayoutParams(
                            new LayoutParams(
                                    LayoutParams.WRAP_CONTENT,
                                    LayoutParams.WRAP_CONTENT));
                    im.setImageResource(android.R.drawable.presence_invisible);

                    TextView tv = new TextView(context);
                    tv.setLayoutParams(
                            new LayoutParams(
                                    LayoutParams.MATCH_PARENT,
                                    LayoutParams.WRAP_CONTENT));
                    tv.setText(selection);

                    layout.addView(im);
                    layout.addView(tv);
                    enqueteLayout.addView(layout);

                    //タップ時のダイアログ
                    final AlertDialog.Builder enqueteAnswerDialogBuilder = new AlertDialog.Builder(context);
                    //タイトル設定
                    enqueteAnswerDialogBuilder.setTitle("アンケートに回答");
                    //ボタン設定
                    enqueteAnswerDialogBuilder.setPositiveButton(R.string.ok_text, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (answerIndex != -1) {
                                SCM.scm.swallow.createAnswer(mInfo.getPostID(), answerIndex);
                                editor.putBoolean("A" + mInfo.getPostID(), true);
                                editor.apply();

                            }
                        }
                    });
                    //選択肢追加
                    enqueteAnswerDialogBuilder.setSingleChoiceItems(enqeteList, -1, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            answerIndex = which;
                        }
                    });
                    final AlertDialog dialog = enqueteAnswerDialogBuilder.create();

                    enqueteLayout.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            String key = "A" + mInfo.getPostID();
                            boolean value = sp.getBoolean(key, false);
                            if (value == false) {
                                //まだ答えていなかったら
                                dialog.show();
                            }
                        }
                    });
                }
            }
        }

        //ふぁぼ
        {
            final int favNum = mInfo.getFavCount();
            final TextView favNumView = (TextView)v.findViewById(R.id.fav_num_text);
            favNumView.setText(String.valueOf(favNum));
            ImageButton starView = (ImageButton)v.findViewById(R.id.star_button);
            starView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    int n = favNum + 1;
                    favNumView.setText(String.valueOf(n));
                    SCM.scm.swallow.createFavorite(MessageView.this.mInfo.getPostID(), n);
                }
            });
            starView.setBackgroundColor(Color.argb(0, 0, 0, 0));
        }

        //タグ
        {
            LinearLayout tagLayout = (LinearLayout)v.findViewById(R.id.tag_layout);
            Integer[] tagIdArray = mInfo.getTagID();
            if (tagIdArray != null) {
                for (int i = 0; i < tagIdArray.length; i++) {
                    String tagName = tvManager.findTagById(tagIdArray[i]).getTagName();
                    TextView textView = new TextView(context);
                    textView.setText(tagName);
                    textView.setTextColor(tagColors[i % tagColors.length]);
                    tagLayout.addView(textView);
                }
            }
        }

        //全体にクリックリスナーを搭載
        this.setOnClickListener(new OnClickListener() {
            public void onClick(View paramView) {
                //キーボードを隠す
                context.hideKeyboard(paramView);
                context.scrollView.requestFocus();
                //アンケート
                if (sp.getString("A"+mInfo.getPostID(), null) == null) {

                }
            }
        });

        //ポップアップが出るようにする
        context.registerForContextMenu(this);

        //このビュー自身のマージン情報を含んだLayoutParams
        lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        lp.setMargins(20, 20, 30, 0);
    }

//    private static final class TagView extends View {
//
//        private static final int[] colors = new int[]
//                {Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW};
//
//        private static final int TEXT_SIZE = 25;
//
//        Paint p = new Paint();
//        int bgColor;
//        String text;
//
//        public TagView(Context context, int index, String text) {
//            super(context);
//            p.setTextSize(TEXT_SIZE);
//            bgColor = colors[index%colors.length];
//            this.text = text;
//            int width = (int)p.measureText(text);
//            this.setLayoutParams(new LayoutParams(TEXT_SIZE + 10, width + 10));
//            this.setBackgroundColor(bgColor);
//        }
//
//        @Override
//        protected void onDraw(Canvas canvas) {
//            super.onDraw(canvas);
//
//            canvas.rotate(90);
//            canvas.translate(0, -TEXT_SIZE-5);
//            p.setColor(Color.WHITE);
//            canvas.drawText(text, 0, TEXT_SIZE, p);
//        }
//
//    }
}
