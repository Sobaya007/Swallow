package com.trap.swallow.talk;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.media.Image;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.trap.swallow.info.TagInfo;
import com.trap.swallow.info.UserInfo;
import com.trap.swallow.server.SCM;
import com.trap.swallow.server.ServerTask;
import com.trap.swallow.server.Swallow;
import com.trap.swallow.server.Swallow.Message;
import com.trap.swallow.server.Swallow.User;
import com.trap.swallow.server.SwallowException;
import com.trap.swallow.swallow.R;

import org.w3c.dom.Text;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;

public class MessageView extends LinearLayout{

    private static final int[] tagColors = {
            Color.rgb(230, 0, 18), Color.rgb(0, 160, 233), Color.rgb(0, 153, 68), Color.rgb(14, 110, 184), Color.rgb(96,25, 134)
    };

    public LayoutParams lp;
    public Message mInfo;

    public int answerIndex;

    public Animation anim;

    private AlertDialog enqueteDialog;
    private AlertDialog answerDialog;

    public MessageView(final TalkActivity context, final Message mInfo, final TalkManager tvManager) {
        super(context);

        this.mInfo = mInfo;

        boolean isYojo = false;
        int yojoTagID = tvManager.findInVisibleTagByName("yojo").tagID;
        for (int tagID : mInfo.getTagID()) {
            if (yojoTagID == tagID) {
                isYojo = true;
                break;
            }
        }

        View v = context.getLayoutInflater().inflate(R.layout.message_view, this);

        //メッセージ
        {
            TextView messageView = (TextView)v.findViewById(R.id.message_text);
            messageView.setText(mInfo.getMessage());
            if (isYojo)
                messageView.setTypeface(MyUtils.yojoFont);
        }

        //リプ
        {
            LinearLayout replyLayout = (LinearLayout)v.findViewById(R.id.reply_text_layout);
            Integer[] reply = mInfo.getReply();
            if (reply != null && reply.length != 0) {
                Calendar c = Calendar.getInstance();
                for (final int postId : reply) {
                    StringBuilder sb = new StringBuilder();
                    Message message = null;
                    try {
                        message = SCM.scm.swallow.findMessage(null, null, null, null, new Integer[]{postId}, null, null, null, null, null, null, null, null)[0];
                    } catch (SwallowException e) {
                        e.printStackTrace();
                    }
                    if (message != null) {
                        sb.append(">>");
                        sb.append(tvManager.findUserById(message.getUserID()).user.getUserName());
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
                        final long until = message.getPosted();
                        replyView.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                AsyncTask<Void, Void, ArrayList<MessageView>> task = new AsyncTask<Void, Void, ArrayList<MessageView>>() {
                                    @Override
                                    protected ArrayList<MessageView> doInBackground(Void... params) {
                                        return tvManager.loadPreviousMessageUntil(until);
                                    }

                                    @Override
                                    protected void onPostExecute(ArrayList<MessageView> messageViews) {
                                        if (messageViews != null) {
                                            for (MessageView mv : messageViews)
                                                tvManager.addMessageViewToPrev(mv);

                                            MessageView mv = tvManager.findMessageViewById(postId);
                                            if (mv != null) {
                                                context.scrollView.smoothScrollTo(0, (int)mv.getY());
                                            }
                                        } else {
                                            MyUtils.showShortToast(context, "読み込みに失敗しました");
                                        }
                                    }
                                };
                                task.execute((Void)null);
                            }
                        });
                    }
                }
            }
        }

        final  UserInfo sender = tvManager.findUserById(mInfo.getUserID());
        //アイコン
        {
            ImageView iconView = (ImageView)v.findViewById(R.id.icon_image);
            iconView.setImageBitmap(sender.profileImage);
            iconView.setOnClickListener(new OnClickListener() {
                public void onClick(View paramView) {
                    AlertDialog.Builder b = new AlertDialog.Builder(context);
                    LinearLayout l = new LinearLayout(context);
                    l.setLayoutParams(MyUtils.getLayoutparams(MyUtils.MP, MyUtils.MP));
                    context.getLayoutInflater().inflate(R.layout.dialog_user, l);
                    TextView userName = (TextView)l.findViewById(R.id.user_name);
                    TextView profile = (TextView)l.findViewById(R.id.user_profile);
                    ImageView imageView = (ImageView)l.findViewById(R.id.user_image);
                    userName.setText(sender.user.getUserName());
                    profile.setText(sender.user.getProfile());
                    imageView.setImageBitmap(sender.profileImage);
                    imageView.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            new ServerTask(context, "画像の読み込みに失敗しました") {
                                @Override
                                public void doInSubThread() throws SwallowException {
                                    Swallow.File file = tvManager.findFileById(sender.user.getImage());
                                    Intent intent = new Intent();
                                    intent.setType(file.getFileType());
                                    intent.setAction(Intent.ACTION_VIEW);
                                    try {
                                        File f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), file.getFileName());
                                        if (f.exists() == false) {
                                            FileOutputStream fos = new FileOutputStream(f);
                                            fos.write(MyUtils.getFileByteArray(file.getFileID()));
                                            fos.flush();
                                            fos.close();
                                        }
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
                            };
                        }
                    });
                    b.setView(l);
                    b.show();
                }
            });
        }

        //名前
        {
            TextView nameView = (TextView)v.findViewById(R.id.name_text);
            nameView.setText(sender.user.getUserName());
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
                            final  String fileType = file.getFileType();
                            final Intent intent = new Intent();
                            intent.setType(fileType);
                            new ServerTask(context, "ファイル読み込みエラー") {
                                @Override
                                public void doInSubThread() throws SwallowException {
                                    if(fileType.equals("application/pdf"))

                                    {
                                        intent.setAction(Intent.ACTION_VIEW);
                                        try {
                                            FileOutputStream fos = new FileOutputStream(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), file.getFileName()));
                                            fos.write(MyUtils.getFileByteArray(file.getFileID()));
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
                                        intent.setDataAndType(Uri.parse(path), "application/pdf");
                                        context.startActivity(intent);
                                    }

                                    else if(fileType.equals("text/plain"))

                                    {
                                        intent.setAction(Intent.ACTION_SEND);
                                        String text = new String(MyUtils.getFileByteArray(file.getFileID()));
                                        intent.putExtra(Intent.EXTRA_TEXT, text);
                                        context.startActivity(intent);
                                    }

                                    else if(fileType.startsWith("image"))

                                    {
                                        intent.setAction(Intent.ACTION_VIEW);
                                        try {
                                            File f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), file.getFileName());
                                            if (f.exists() == false) {
                                                FileOutputStream fos = new FileOutputStream(f);
                                                fos.write(MyUtils.getFileByteArray(file.getFileID()));
                                                fos.flush();
                                                fos.close();
                                            }
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


                            };
                        }
                    });
                    final ImageView iv = new ImageView(context);
                    iv.setLayoutParams(MyUtils.getLayoutparams(100, 100));
                    String fileType = file.getFileType();
                    if (fileType.startsWith("image")) {
                        AsyncTask<Void, Void, byte[]> task = new AsyncTask<Void, Void, byte[]>() {
                            @Override
                            protected byte[] doInBackground(Void... params) {
                                byte[] buf = MyUtils.getThumbnailByteArray(id, 100, 100);
                                return buf;
                            }

                            @Override
                            protected void onPostExecute(byte[] bytes) {
                                if (bytes != null) {
                                    iv.setImageBitmap(BitmapFactory.decodeStream(new ByteArrayInputStream(bytes)));
                                }
                            }
                        };
                        task.execute((Void)null);
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

                    //タップ時のアンケート回答用ダイアログ
                    final AlertDialog.Builder enqueteDialogBuilder = new AlertDialog.Builder(context);
                    //タイトル設定
                    enqueteDialogBuilder.setTitle("アンケートに回答");
                    //ボタン設定
                    enqueteDialogBuilder.setPositiveButton(R.string.ok_text, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (answerIndex != -1) {
                                new ServerTask(context, "アンケート回答失敗です") {
                                    @Override
                                    public void doInSubThread() throws SwallowException {
                                        SCM.scm.swallow.createAnswer(mInfo.getPostID(), answerIndex);
                                        SharedPreferences.Editor editor = MyUtils.sp.edit();
                                        editor.putBoolean("A" + mInfo.getPostID(), true);
                                        editor.apply();
                                    }
                                };
                            }
                        }
                    });
                    //選択肢追加
                    enqueteDialogBuilder.setSingleChoiceItems(enqeteList, -1, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            answerIndex = which;
                        }
                    });

                    final AlertDialog.Builder answerDialogBuilder = new AlertDialog.Builder(context);
                    //タイトル設定
                    answerDialogBuilder.setTitle("アンケートの回答");

                    enqueteLayout.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            //その投稿が自分のものかどうか
                            if (mInfo.getUserID() == tvManager.getMyUserInfo().user.getUserID()) {
                                //自分のものであったなら
                                final ArrayList[] answers = new ArrayList[enqeteList.length];
                                for (int i = 0; i < answers.length; i++)
                                    answers[i] = new ArrayList();
                                new ServerTask(context, "アンケート結果の取得に失敗しました") {
                                    @Override
                                    public void doInSubThread() throws SwallowException {
                                        //回答者名を回答ごとに仕分け
                                        Message m = SCM.scm.swallow.findMessage(null, null, null, null, new Integer[]{mInfo.getPostID()}, null, null, null, null, null, null, null, null)[0];
                                        for (Swallow.Answer a : m.getAnswer()) {
                                            answers[a.getAnswer()].add(a.getUserID());
                                        }
                                    }

                                    @Override
                                    protected void onPostExecute(Boolean aBoolean) {
                                        if (aBoolean) {
                                            EnqueteView[] views = new EnqueteView[enqeteList.length];
                                            for (int i = 0; i < views.length; i++) {
                                                views[i] = new EnqueteView(context, i, answers[i], enqeteList[i]);
                                            }
                                            EnqueteAdapter adapter = new EnqueteAdapter(context,views);
                                            ListView listView = new ListView(context);
                                            listView.setAdapter(adapter);
                                            listView.setLayoutParams(MyUtils.getLayoutparams(MyUtils.MP, MyUtils.MP));
                                            answerDialogBuilder.setView(listView);
                                            answerDialog = answerDialogBuilder.create();
                                            answerDialog.show();
                                        }
                                    }
                                };
                            } else {
                                //自分のものでなかったら
                                //既に答えているかどうかPreferenceで確認
                                String key = MyUtils.ENQUETE_ANSWER_KEY + mInfo.getPostID();
                                boolean value = MyUtils.sp.getBoolean(key, false);
                                if (value == false) {
                                    //まだ答えていなかったら
                                    if (enqueteDialog == null)
                                        enqueteDialog = enqueteDialogBuilder.create();
                                    enqueteDialog.show();
                                }
                            }
                        }
                    });
                }
            }
        }
        //ふぁぼ
        {
            final TextView favNumView = (TextView)v.findViewById(R.id.fav_num_text);
            favNumView.setText(String.valueOf(mInfo.getFavCount()));

            ImageButton favButton = (ImageButton)v.findViewById(R.id.fav_button);
            favButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    new ServerTask(context, "ふぁぼに失敗しました") {
                        @Override
                        public void doInSubThread() throws SwallowException {
                            SCM.scm.swallow.createFavorite(MessageView.this.mInfo.getPostID(), 1);
                            MessageView.this.mInfo = SCM.scm.swallow.findMessage(null, null, null, null, new Integer[]{MessageView.this.mInfo.getPostID()}, null, null, null, null, null, null, null, null)[0];
                        }
                    };
                    favNumView.setText(String.valueOf(MessageView.this.mInfo.getFavCount()));
                }
            });
        }

        //タグ
        {
            LinearLayout tagLayout = (LinearLayout)v.findViewById(R.id.tag_layout);
            Integer[] tagIdArray = mInfo.getTagID();
            if (tagIdArray != null) {
                for (int i = 0; i < tagIdArray.length; i++) {
                    TagInfo tag = tvManager.findVisibleTagById(tagIdArray[i]);
                    if (tag != null) {
                        String tagName = tag.tagName;
                        TextView textView = new TextView(context);
                        textView.setText(tagName);
                        textView.setTextColor(tagColors[i % tagColors.length]);
                        textView.setGravity(Gravity.CENTER);
                        textView.setTextSize(10);
                        tagLayout.addView(textView);
                    }
                }
            }
        }

        //既読
        {
            boolean flag = false;
            for (int tagID : mInfo.getTagID()) {
                TagInfo tag = tvManager.findInvisibleTagById(tagID);
                if (tag != null) {
                    if (tag.tagName.equals("confirmation")){
                        flag = true;
                        break;
                    }
                }
            }
            ImageButton iv = (ImageButton)findViewById(R.id.receivedButton);
            if (flag) {
                iv.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //タッチ時のダイアログを作成
                        new ServerTask(context, "既読の取得に失敗しました") {
                            @Override
                            public void doInSubThread() throws SwallowException {
                                MessageView.this.mInfo = SCM.scm.swallow.findMessage(null, null, null, null, new Integer[]{mInfo.getPostID()}, null, null, null, null, null, null, null, null)[0];
                            }

                            @Override
                            protected void onPostExecute(Boolean aBoolean) {
                                if (aBoolean) {
                                    Message mInfo = MessageView.this.mInfo;
                                    if (mInfo.getReceived() != null) {
                                        LinearLayout layout = new LinearLayout(context);
                                        layout.setLayoutParams(MyUtils.getLayoutparams(MyUtils.MP, MyUtils.WC));
                                        layout.setOrientation(LinearLayout.HORIZONTAL);
                                        for (Swallow.Received received : mInfo.getReceived()) {
                                            UserInfo user = context.tvManager.findUserById(received.getUserID());
                                            ImageView iv = new ImageView(context);
                                            iv.setLayoutParams(MyUtils.getLayoutparams(100, 100));
                                            if (user.user.getImage() != null) {
                                                iv.setImageBitmap(user.profileImage);
                                            }
                                            TextView tv = new TextView(context);
                                            tv.setLayoutParams(MyUtils.getLayoutparams(MyUtils.WC, MyUtils.WC));
                                            tv.setTextSize(16);
                                            tv.setTextColor(Color.BLACK);
                                            tv.setText(user.user.getUserName());
                                            LinearLayout l = new LinearLayout(context);
                                            l.setLayoutParams(MyUtils.getLayoutparams(MyUtils.WC, MyUtils.WC));
                                            l.setOrientation(LinearLayout.VERTICAL);
                                            l.addView(iv);
                                            l.addView(tv);
                                            layout.addView(l);
                                        }
                                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                                        builder.setView(layout);
                                        final AlertDialog dialog = builder.create();
                                        dialog.show();
                                    }
                                }
                            }
                        };
                    }
                });
            } else {
                ((LinearLayout)iv.getParent()).removeView(iv);
            }
        }
        //リプボタン
        {
            ImageButton replyButton = (ImageButton)findViewById(R.id.reply_button);
            replyButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    tvManager.startReply(mInfo.getPostID());
                    context.shiftToInputForm();
                }
            });
        }

        //全体にクリックリスナーを搭載
        this.setOnClickListener(new OnClickListener() {
            public void onClick(View paramView) {
                //キーボードを隠す
                context.hideKeyboard(paramView);
                context.scrollView.requestFocus();
                //アンケート
                String value = MyUtils.sp.getString("A" + mInfo.getPostID(), null);
                if (value == null) {

                }
            }
        });

        //ポップアップが出るようにする
        context.registerForContextMenu(this);

        //このビュー自身のマージン情報を含んだLayoutParams
        lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        lp.setMargins(20, 20, 30, 0);
    }

    public final void refleshOnUserInfoChanged() {
        boolean isYojo = false;
        int yojoTagID = TalkActivity.singleton.tvManager.findInVisibleTagByName("yojo").tagID;
        for (int tagID : mInfo.getTagID()) {
            if (yojoTagID == tagID) {
                isYojo = true;
                break;
            }
        }
        View v = this.getChildAt(0);
        final  UserInfo sender = TalkActivity.singleton.tvManager.findUserById(mInfo.getUserID());
        //アイコン
        {
            ImageView iconView = (ImageView)v.findViewById(R.id.icon_image);
            iconView.setImageBitmap(sender.profileImage);
        }

        //名前
        {
            TextView nameView = (TextView)v.findViewById(R.id.name_text);
            nameView.setText(sender.user.getUserName());
        }
    }

    private static final class EnqueteAdapter extends BaseAdapter {

        private final EnqueteView[] views;

        public EnqueteAdapter(Context context, EnqueteView[] views) {
            this.views = views;
        }

        @Override
        public int getCount() {
            return views.length;
        }

        @Override
        public Object getItem(int position) {
            return views[position];
        }

        @Override
        public long getItemId(int position) {
            return views[position].getId();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return views[position];
        }
    }

    private static final class EnqueteView extends View {

        private static final float MAX_NUM = 30; //この数でビューいっぱいの長さ

        private final Paint paint;
        private final int num;
        private final String str;

        public EnqueteView(final TalkActivity context, int index, final ArrayList<Integer> answerPersonID, String str) {
            super(context);
            this.paint = new Paint();
            paint.setColor(tagColors[index]);
            paint.setTextSize(50);
            this.num = answerPersonID.size();
            this.str = str;
            //画面サイズ取得
            WindowManager wm = (WindowManager)context.getSystemService(TalkActivity.WINDOW_SERVICE);
            Display disp = wm.getDefaultDisplay();
            Point size = new Point();
            disp.getSize(size);
            this.setLayoutParams(new AbsListView.LayoutParams((int) (size.x * 0.8), (int) (size.y * 0.1)));

            //タッチ時のダイアログを作成
            LinearLayout layout = new LinearLayout(context);
            layout.setLayoutParams(MyUtils.getLayoutparams(MyUtils.MP, MyUtils.WC));
            layout.setOrientation(LinearLayout.HORIZONTAL);
            for (int ID : answerPersonID) {
                UserInfo user = context.tvManager.findUserById(ID);
                ImageView iv = new ImageView(context);
                iv.setLayoutParams(MyUtils.getLayoutparams(100, 100));
                if (user.user.getImage() != null) {
                    iv.setImageBitmap(user.profileImage);
                }
                TextView tv = new TextView(context);
                tv.setLayoutParams(MyUtils.getLayoutparams(MyUtils.WC, MyUtils.WC));
                tv.setTextSize(16);
                tv.setTextColor(Color.BLACK);
                tv.setText(user.user.getUserName());
                LinearLayout l = new LinearLayout(context);
                l.setLayoutParams(MyUtils.getLayoutparams(MyUtils.WC, MyUtils.WC));
                l.setOrientation(LinearLayout.VERTICAL);
                l.addView(iv);
                l.addView(tv);
                layout.addView(l);
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setView(layout);
            final AlertDialog dialog = builder.create();
            this.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (v == EnqueteView.this)
                        dialog.show();
                }
            });
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            Rect bounds = new Rect();
            Paint.FontMetrics fm = paint.getFontMetrics();
            float left;
            //数字を書く
            {
                String text = Integer.toString(num);
                paint.getTextBounds(text, 0, text.length(), bounds);
                canvas.drawText(text, left = (getWidth() - bounds.width() - 10), getHeight() * 0.35f - 0.5f * (fm.bottom + fm.top), paint);
            }
            //棒をぬる
            canvas.drawRect(left - getWidth() * num / MAX_NUM - 30, getHeight() * 0.1f, getRight() + left - getWidth() - 30, getHeight() * 0.6f, paint);

            //文字を書く
            {
                paint.getTextBounds(str, 0, str.length(), bounds);
                canvas.drawText(str, (getWidth() - bounds.width()) / 2, getHeight() * 0.6f - fm.top, paint);
            }
        }
    }
}
