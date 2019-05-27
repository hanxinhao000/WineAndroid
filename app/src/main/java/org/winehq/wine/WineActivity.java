package org.winehq.wine;

import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.PointerIcon;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;

import org.winehq.wine.gameio.GameIo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

public class WineActivity extends AppCompatActivity {

    private final String LOGTAG = "wine";
    private PointerIcon current_cursor;
    protected WineWindow desktop_window;
    protected WineWindow message_window;
    private ProgressDialog progress_dialog;
    private HashMap<Integer, WineWindow> win_map = new HashMap();

    protected class TopView extends RelativeLayout {
        public TopView(Context context, int hwnd) {
            super(context);
            WineActivity.this.desktop_window = new WineWindow(hwnd, null, 1.0f);
            addView(WineActivity.this.desktop_window.create_whole_view());
            WineActivity.this.desktop_window.client_group.bringToFront();
            WineActivity.this.message_window = new WineWindow(-3, null, 1.0f);
            WineActivity.this.message_window.create_window_groups();
            View inflate = View.inflate(context, R.layout.activity_wine2, null);


            addView(inflate);
        }

        protected void onSizeChanged(int width, int height, int old_width, int old_height) {
            Log.i("wine", String.format("desktop size %dx%d", new Object[]{Integer.valueOf(width), Integer.valueOf(height)}));
            WineActivity.this.wine_desktop_changed(width, height);
        }

        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        }
    }

    protected class WineView extends TextureView implements TextureView.SurfaceTextureListener {
        private boolean is_client;
        private WineWindow window;

        public WineView(Context c, WineWindow win, boolean client) {
            super(c);
            this.window = win;
            this.is_client = client;
            setSurfaceTextureListener(this);
            setVisibility(VISIBLE);
            setOpaque(false);
            setFocusable(true);
            setFocusableInTouchMode(true);
        }

        public WineWindow get_window() {
            return this.window;
        }

        public void onSurfaceTextureAvailable(SurfaceTexture surftex, int width, int height) {
            String str = "wine";
            String str2 = "onSurfaceTextureAvailable win %08x %dx%d %s";
            Object[] objArr = new Object[4];
            objArr[0] = Integer.valueOf(this.window.hwnd);
            objArr[1] = Integer.valueOf(width);
            objArr[2] = Integer.valueOf(height);
            objArr[3] = this.is_client ? "client" : "whole";
            Log.i(str, String.format(str2, objArr));
            this.window.set_surface(surftex, this.is_client);
        }

        public void onSurfaceTextureSizeChanged(SurfaceTexture surftex, int width, int height) {
            String str = "wine";
            String str2 = "onSurfaceTextureSizeChanged win %08x %dx%d %s";
            Object[] objArr = new Object[4];
            objArr[0] = Integer.valueOf(this.window.hwnd);
            objArr[1] = Integer.valueOf(width);
            objArr[2] = Integer.valueOf(height);
            objArr[3] = this.is_client ? "client" : "whole";
            Log.i(str, String.format(str2, objArr));
            this.window.set_surface(surftex, this.is_client);
        }

        public boolean onSurfaceTextureDestroyed(SurfaceTexture surftex) {
            String str = "wine";
            String str2 = "onSurfaceTextureDestroyed win %08x %s";
            Object[] objArr = new Object[2];
            objArr[0] = Integer.valueOf(this.window.hwnd);
            objArr[1] = this.is_client ? "client" : "whole";
            Log.i(str, String.format(str2, objArr));
            this.window.set_surface(null, this.is_client);
            return false;
        }

        public void onSurfaceTextureUpdated(SurfaceTexture surftex) {
        }

        @TargetApi(24)
        public PointerIcon onResolvePointerIcon(MotionEvent event, int index) {
            return WineActivity.this.current_cursor;
        }

        public boolean onGenericMotionEvent(MotionEvent event) {
            if (this.is_client) {
                return false;
            }
            if (this.window.parent != null && this.window.parent != WineActivity.this.desktop_window) {
                return false;
            }
            if ((event.getSource() & 2) == 0) {
                return super.onGenericMotionEvent(event);
            }
            int[] pos = new int[2];
            this.window.get_event_pos(event, pos);
            Log.i("wine", String.format("view motion event win %08x action %d pos %d,%d buttons %04x view %d,%d", new Object[]{Integer.valueOf(this.window.hwnd), Integer.valueOf(event.getAction()), Integer.valueOf(pos[0]), Integer.valueOf(pos[1]), Integer.valueOf(event.getButtonState()), Integer.valueOf(getLeft()), Integer.valueOf(getTop())}));
            return WineActivity.this.wine_motion_event(this.window.hwnd, event.getAction(), pos[0], pos[1], event.getButtonState(), (int) event.getAxisValue(9));
        }

        public boolean onTouchEvent(MotionEvent event) {
            if (this.is_client) {
                return false;
            }
            if (this.window.parent != null && this.window.parent != WineActivity.this.desktop_window) {
                return false;
            }
            int[] pos = new int[2];
            this.window.get_event_pos(event, pos);
            Log.i("wine", String.format("view touch event win %08x action %d pos %d,%d buttons %04x view %d,%d", new Object[]{Integer.valueOf(this.window.hwnd), Integer.valueOf(event.getAction()), Integer.valueOf(pos[0]), Integer.valueOf(pos[1]), Integer.valueOf(event.getButtonState()), Integer.valueOf(getLeft()), Integer.valueOf(getTop())}));
            return WineActivity.this.wine_motion_event(this.window.hwnd, event.getAction(), pos[0], pos[1], event.getButtonState(), 0);
        }

        //传入键盘事件
        public boolean dispatchKeyEvent(KeyEvent event) {
            Log.i("wine", String.format("view key event win %08x action %d keycode %d (%s)", new Object[]{Integer.valueOf(this.window.hwnd), Integer.valueOf(event.getAction()), Integer.valueOf(event.getKeyCode()), KeyEvent.keyCodeToString(event.getKeyCode())}));

            boolean ret = WineActivity.this.wine_keyboard_event(this.window.hwnd, event.getAction(), event.getKeyCode(), event.getMetaState());
            if (ret) {
                return ret;
            }
            return super.dispatchKeyEvent(event);
        }
    }

    protected class WineWindow {
        protected static final int HWND_MESSAGE = -3;
        protected static final int SWP_NOZORDER = 4;
        protected static final int WS_VISIBLE = 268435456;
        protected ArrayList<WineWindow> children;
        protected WineWindowGroup client_group;
        protected Rect client_rect;
        protected Surface client_surface;
        protected SurfaceTexture client_surftex;
        protected int hwnd;
        protected int owner = 0;
        protected WineWindow parent;
        protected float scale;
        protected int style = 0;
        protected boolean visible = false;
        protected Rect visible_rect;
        protected WineWindowGroup window_group;
        protected Surface window_surface;
        protected SurfaceTexture window_surftex;

        public WineWindow(int w, WineWindow parent, float scale) {
            Log.i("wine", String.format("create hwnd %08x", new Object[]{Integer.valueOf(w)}));
            this.hwnd = w;
            Rect rect = new Rect(0, 0, 0, 0);
            this.client_rect = rect;
            this.visible_rect = rect;
            this.parent = parent;
            this.scale = scale;
            this.children = new ArrayList();
            WineActivity.this.win_map.put(Integer.valueOf(w), this);
            if (parent != null) {
                parent.children.add(this);
            }
        }

        public void destroy() {
            Log.i("wine", String.format("destroy hwnd %08x", new Object[]{Integer.valueOf(this.hwnd)}));
            this.visible = false;
            WineActivity.this.win_map.remove(this);
            if (this.parent != null) {
                this.parent.children.remove(this);
            }
            destroy_window_groups();
        }

        public WineWindowGroup create_window_groups() {
            if (this.client_group != null) {
                return this.client_group;
            }
            this.window_group = new WineWindowGroup(this);
            this.client_group = new WineWindowGroup(this);
            this.window_group.addView(this.client_group);
            this.client_group.set_layout(this.client_rect.left - this.visible_rect.left, this.client_rect.top - this.visible_rect.top, this.client_rect.right - this.visible_rect.left, this.client_rect.bottom - this.visible_rect.top);
            if (this.parent != null) {
                this.parent.create_window_groups();
                if (this.visible) {
                    add_view_to_parent();
                }
                this.window_group.set_layout(this.visible_rect.left, this.visible_rect.top, this.visible_rect.right, this.visible_rect.bottom);
            }
            return this.client_group;
        }

        public void destroy_window_groups() {
            if (this.window_group != null) {
                if (!(this.parent == null || this.parent.client_group == null)) {
                    remove_view_from_parent();
                }
                this.window_group.destroy_view();
            }
            if (this.client_group != null) {
                this.client_group.destroy_view();
            }
            this.window_group = null;
            this.client_group = null;
        }

        public View create_whole_view() {
            if (this.window_group == null) {
                create_window_groups();
            }
            this.window_group.create_view(false).layout(0, 0, Math.round(((float) (this.visible_rect.right - this.visible_rect.left)) * this.scale), Math.round(((float) (this.visible_rect.bottom - this.visible_rect.top)) * this.scale));
            this.window_group.set_scale(this.scale);
            return this.window_group;
        }

        public void create_client_view() {
            if (this.client_group == null) {
                create_window_groups();
            }
            Log.i("wine", String.format("creating client view %08x %s", new Object[]{Integer.valueOf(this.hwnd), this.client_rect}));
            this.client_group.create_view(true).layout(0, 0, this.client_rect.right - this.client_rect.left, this.client_rect.bottom - this.client_rect.top);
        }

        protected void add_view_to_parent() {
            int pos = this.parent.client_group.getChildCount() - 1;
            if (pos >= 0 && this.parent.client_group.getChildAt(pos) == this.parent.client_group.get_content_view()) {
                pos--;
            }
            for (int i = 0; i < this.parent.children.size() && pos >= 0; i++) {
                WineWindow child = (WineWindow) this.parent.children.get(i);
                if (child == this) {
                    break;
                }
                if (child.visible && child == ((WineWindowGroup) this.parent.client_group.getChildAt(pos)).get_window()) {
                    pos--;
                }
            }
            this.parent.client_group.addView(this.window_group, pos + 1);
        }

        protected void remove_view_from_parent() {
            this.parent.client_group.removeView(this.window_group);
        }

        protected void set_zorder(WineWindow prev) {
            int pos = -1;
            this.parent.children.remove(this);
            if (prev != null) {
                pos = this.parent.children.indexOf(prev);
            }
            this.parent.children.add(pos + 1, this);
        }

        protected void sync_views_zorder() {
            int i;
            int i2 = this.parent.children.size() - 1;
            int j = 0;
            while (i2 >= 0) {
                WineWindow child = (WineWindow) this.parent.children.get(i2);
                if (child.visible) {
                    View child_view = this.parent.client_group.getChildAt(j);
                    if (child_view == this.parent.client_group.get_content_view()) {
                        continue;
                    } else if (child != ((WineWindowGroup) child_view).get_window()) {
                        i = i2;
                        break;
                    } else {
                        j++;
                    }
                }
                i2--;
            }
            i = i2;
            while (i >= 0) {
                i2 = i - 1;
                WineWindow child = (WineWindow) this.parent.children.get(i);
                if (child.visible) {
                    child.window_group.bringToFront();
                }
                i = i2;
            }
        }

        public void pos_changed(int flags, int insert_after, int owner, int style, Rect window_rect, Rect client_rect, Rect visible_rect) {
            boolean was_visible = this.visible;
            this.visible_rect = visible_rect;
            this.client_rect = client_rect;
            this.style = style;
            this.owner = owner;
            this.visible = (style & WS_VISIBLE) != 0;
            Log.i("wine", String.format("pos changed hwnd %08x after %08x owner %08x style %08x win %s client %s visible %s flags %08x", new Object[]{Integer.valueOf(this.hwnd), Integer.valueOf(insert_after), Integer.valueOf(owner), Integer.valueOf(style), window_rect, client_rect, visible_rect, Integer.valueOf(flags)}));
            if ((flags & SWP_NOZORDER) == 0 && this.parent != null) {
                set_zorder(WineActivity.this.get_window(insert_after));
            }
            if (this.window_group != null) {
                this.window_group.set_layout(visible_rect.left, visible_rect.top, visible_rect.right, visible_rect.bottom);
                if (this.parent != null) {
                    if (!was_visible && (style & WS_VISIBLE) != 0) {
                        add_view_to_parent();
                    } else if (was_visible && (style & WS_VISIBLE) == 0) {
                        remove_view_from_parent();
                    } else if (this.visible && (flags & SWP_NOZORDER) == 0) {
                        sync_views_zorder();
                    }
                }
            }
            if (this.client_group != null) {
                this.client_group.set_layout(client_rect.left - visible_rect.left, client_rect.top - visible_rect.top, client_rect.right - visible_rect.left, client_rect.bottom - visible_rect.top);
            }
        }

        public void set_parent(WineWindow new_parent, float scale) {
            Log.i("wine", String.format("set parent hwnd %08x parent %08x -> %08x", new Object[]{Integer.valueOf(this.hwnd), Integer.valueOf(this.parent.hwnd), Integer.valueOf(new_parent.hwnd)}));
            this.scale = scale;
            if (this.window_group != null) {
                if (this.visible) {
                    remove_view_from_parent();
                }
                new_parent.create_window_groups();
                this.window_group.set_layout(this.visible_rect.left, this.visible_rect.top, this.visible_rect.right, this.visible_rect.bottom);
            }
            this.parent.children.remove(this);
            this.parent = new_parent;
            this.parent.children.add(this);
            if (this.visible && this.window_group != null) {
                add_view_to_parent();
            }
        }

        public int get_hwnd() {
            return this.hwnd;
        }

        private void update_surface(boolean is_client) {
            if (is_client) {
                Log.i("wine", String.format("set client surface hwnd %08x %s", new Object[]{Integer.valueOf(this.hwnd), this.client_surface}));
                if (this.client_surface != null) {
                    WineActivity.this.wine_surface_changed(this.hwnd, this.client_surface, true);
                    return;
                }
                return;
            }
            Log.i("wine", String.format("set window surface hwnd %08x %s", new Object[]{Integer.valueOf(this.hwnd), this.window_surface}));
            if (this.window_surface != null) {
                WineActivity.this.wine_surface_changed(this.hwnd, this.window_surface, false);
            }
        }

        public void set_surface(SurfaceTexture surftex, boolean is_client) {
            if (is_client) {
                if (surftex == null) {
                    this.client_surface = null;
                } else if (surftex != this.client_surftex) {
                    this.client_surftex = surftex;
                    this.client_surface = new Surface(surftex);
                }
            } else if (surftex == null) {
                this.window_surface = null;
            } else if (surftex != this.window_surftex) {
                this.window_surftex = surftex;
                this.window_surface = new Surface(surftex);
            }
            update_surface(is_client);
        }

        public void get_event_pos(MotionEvent event, int[] pos) {
            pos[0] = Math.round((event.getX() * this.scale) + ((float) this.window_group.getLeft()));
            pos[1] = Math.round((event.getY() * this.scale) + ((float) this.window_group.getTop()));
        }
    }

    protected class WineWindowGroup extends ViewGroup {
        private WineView content_view;
        private WineWindow win;

        WineWindowGroup(WineWindow win) {
            super(WineActivity.this);
            this.win = win;
            setVisibility(VISIBLE);
        }

        public void set_layout(int left, int top, int right, int bottom) {
            left = (int) (((float) left) * this.win.scale);
            top = (int) (((float) top) * this.win.scale);
            right = (int) (((float) right) * this.win.scale);
            bottom = (int) (((float) bottom) * this.win.scale);
            if (right <= left + 1) {
                right = left + 2;
            }
            if (bottom <= top + 1) {
                bottom = top + 2;
            }
            layout(left, top, right, bottom);
        }

        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            if (this.content_view != null) {
                this.content_view.layout(0, 0, right - left, bottom - top);
            }
        }

        public void set_scale(float scale) {
            if (this.content_view != null) {
                this.content_view.setPivotX(0.0f);
                this.content_view.setPivotY(0.0f);
                this.content_view.setScaleX(scale);
                this.content_view.setScaleY(scale);
            }
        }

        public WineView create_view(boolean is_client) {
            if (this.content_view != null) {
                return this.content_view;
            }
            this.content_view = new WineView(WineActivity.this, this.win, is_client);
            addView(this.content_view);
            if (!is_client) {
                this.content_view.setFocusable(true);
                this.content_view.setFocusableInTouchMode(true);
            }
            return this.content_view;
        }

        public void destroy_view() {
            if (this.content_view != null) {
                removeView(this.content_view);
                this.content_view = null;
            }
        }

        public WineView get_content_view() {
            return this.content_view;
        }

        public WineWindow get_window() {
            return this.win;
        }
    }

    private native String wine_init(String[] strArr, String[] strArr2);

    public native void wine_config_changed(int i);

    public native void wine_desktop_changed(int i, int i2);

    public native boolean wine_keyboard_event(int i, int i2, int i3, int i4);

    public native boolean wine_motion_event(int i, int i2, int i3, int i4, int i5, int i6);

    public native void wine_surface_changed(int i, Surface surface, boolean z);

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(1);
        new Thread(new Runnable() {
            public void run() {
                WineActivity.this.loadWine(null);
            }
        }).start();
    }

    @TargetApi(21)
    private String[] get_supported_abis() {
        if (Build.VERSION.SDK_INT >= 21) {
            return Build.SUPPORTED_ABIS;
        }
        return new String[]{Build.CPU_ABI};
    }

    private String get_wine_abi() {
        for (String abi : get_supported_abis()) {
            if (new File(getFilesDir(), abi + "/bin/wineserver").canExecute()) {
                return abi;
            }
        }
        Log.e("wine", "could not find a supported ABI");
        return null;
    }

    private void loadWine(String cmdline) {
        copyAssetFiles();
        String wine_abi = get_wine_abi();
        File bindir = new File(getFilesDir(), wine_abi + "/bin");
        File libdir = new File(getFilesDir(), wine_abi + "/lib");
        File dlldir = new File(libdir, "wine");
        File prefix = new File(getFilesDir(), "prefix");
        File loader = new File(bindir, "wine");
        String locale = Locale.getDefault().getLanguage() + "_" + Locale.getDefault().getCountry() + ".UTF-8";
        HashMap<String, String> env = new HashMap();
        env.put("WINELOADER", loader.toString());
        env.put("WINEPREFIX", prefix.toString());
        env.put("WINEDLLPATH", dlldir.toString());
        env.put("LD_LIBRARY_PATH", libdir.toString() + ":" + getApplicationInfo().nativeLibraryDir);
        env.put("LC_ALL", locale);
        env.put("LANG", locale);
        env.put("PATH", bindir.toString() + ":" + System.getenv("PATH"));
        if (cmdline == null) {
            if (new File(prefix, "drive_c/winestart.cmd").exists()) {
                cmdline = "c:\\winestart.cmd";

            } else {
                cmdline = "wineconsole.exe";
               // cmdline = "c:\\360zip.exe";
            }
        }
        String winedebug = readFileString(new File(prefix, "winedebug"));
        if (winedebug == null) {
            winedebug = readFileString(new File(getFilesDir(), "winedebug"));
        }
        if (winedebug != null) {
            File log = new File(getFilesDir(), "log");
            env.put("WINEDEBUG", winedebug);
            env.put("WINEDEBUGLOG", log.toString());
            Log.i("wine", "logging to " + log.toString());
            log.delete();
        }
        createProgressDialog(0, "Setting up the Windows environment...");
        try {
            System.loadLibrary("wine");
        } catch (UnsatisfiedLinkError e) {
            System.load(libdir.toString() + "/libwine.so");
        }
        prefix.mkdirs();
        runWine(cmdline, env);
    }

    private final void runWine(String cmdline, HashMap<String, String> environ) {
        String[] env = new String[(environ.size() * 2)];
        int j = 0;
        for (Map.Entry<String, String> entry : environ.entrySet()) {
            int i = j + 1;
            env[j] = (String) entry.getKey();
            j = i + 1;
            env[i] = (String) entry.getValue();
        }
        String str = "wine";


        Log.e("哈哈哈哈", wine_init(new String[]{(String) environ.get("WINELOADER"), "explorer.exe", "/desktop=shell,,android", cmdline}, env));
    }

    private void createProgressDialog(final int max, final String message) {
        runOnUiThread(new Runnable() {
            public void run() {
                if (WineActivity.this.progress_dialog != null) {
                    WineActivity.this.progress_dialog.dismiss();
                }
                WineActivity.this.progress_dialog = new ProgressDialog(WineActivity.this);
                WineActivity.this.progress_dialog.setProgressStyle(max > 0 ? 1 : 0);
                WineActivity.this.progress_dialog.setTitle("Wine");
                WineActivity.this.progress_dialog.setMessage(message);
                WineActivity.this.progress_dialog.setCancelable(false);
                WineActivity.this.progress_dialog.setMax(max);
                WineActivity.this.progress_dialog.show();
            }
        });
    }

    private final boolean isFileWanted(String name) {
        if (name.equals("files.sum") || name.startsWith("share/")) {
            return true;
        }
        for (String abi : get_supported_abis()) {
            if (name.startsWith(abi + "/system/")) {
                return false;
            }
            if (name.startsWith(abi + "/")) {
                return true;
            }
        }
        if (name.startsWith("x86/")) {
            return true;
        }
        return false;
    }

    private final boolean isFileExecutable(String name) {
        return (name.equals("files.sum") || name.startsWith("share/")) ? false : true;
    }

    private final HashMap<String, String> readMapFromInputStream(InputStream in) {
        HashMap<String, String> map = new HashMap();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            while (true) {
                String str = reader.readLine();
                if (str == null) {
                    break;
                }
                String[] entry = str.split("\\s+", 2);
                if (entry.length == 2 && isFileWanted(entry[1])) {
                    map.put(entry[1], entry[0]);
                }
            }
        } catch (IOException e) {
        }
        return map;
    }

    private final HashMap<String, String> readMapFromDiskFile(String file) {
        try {
            return readMapFromInputStream(new FileInputStream(new File(getFilesDir(), file)));
        } catch (IOException e) {
            return new HashMap();
        }
    }

    private final HashMap<String, String> readMapFromAssetFile(String file) {
        try {
            return readMapFromInputStream(getAssets().open(file));
        } catch (IOException e) {
            return new HashMap();
        }
    }

    private final String readFileString(File file) {
        try {
            return new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8")).readLine();
        } catch (IOException e) {
            return null;
        }
    }

    private final void copyAssetFile(String src) {
        File dest = new File(getFilesDir(), src);
        try {
            Log.i("wine", "extracting " + dest);
            dest.getParentFile().mkdirs();
            dest.delete();
            if (dest.createNewFile()) {
                InputStream in = getAssets().open(src);
                FileOutputStream out = new FileOutputStream(dest);
                byte[] buffer = new byte[65536];
                while (true) {
                    int read = in.read(buffer);
                    if (read <= 0) {
                        break;
                    }
                    out.write(buffer, 0, read);
                }
                out.close();
                if (isFileExecutable(src)) {
                    dest.setExecutable(true, true);
                    return;
                }
                return;
            }
            Log.i("wine", "Failed to create file " + dest);
        } catch (IOException e) {
            Log.i("wine", "Failed to copy asset file to " + dest);
            dest.delete();
        }
    }

    private final void deleteAssetFile(String src) {
        File dest = new File(getFilesDir(), src);
        Log.i("wine", "deleting " + dest);
        dest.delete();
    }

    private final void copyAssetFiles() {
        String new_sum = (String) readMapFromAssetFile("sums.sum").get("files.sum");
        if (new_sum != null) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            if (!prefs.getString("files.sum", BuildConfig.FLAVOR).equals(new_sum)) {
                String name;
                prefs.edit().putString("files.sum", new_sum).apply();
                HashMap<String, String> existing_files = readMapFromDiskFile("files.sum");
                HashMap<String, String> new_files = readMapFromAssetFile("files.sum");
                ArrayList<String> copy_files = new ArrayList();
                copy_files.add("files.sum");
                for (Map.Entry<String, String> entry : new_files.entrySet()) {
                    name = (String) entry.getKey();
                    if (!((String) entry.getValue()).equals(existing_files.remove(name))) {
                        copy_files.add(name);
                    }
                }
                createProgressDialog(copy_files.size(), "Extracting files...");
                for (String name2 : existing_files.keySet()) {
                    deleteAssetFile(name2);
                }
                Iterator it = copy_files.iterator();
                while (it.hasNext()) {
                    copyAssetFile((String) it.next());
                    runOnUiThread(new Runnable() {
                        public void run() {
                            WineActivity.this.progress_dialog.incrementProgressBy(1);
                        }
                    });
                }
            }
        }
    }

    protected WineWindow get_window(int hwnd) {
        return (WineWindow) this.win_map.get(Integer.valueOf(hwnd));
    }

    //标记1
    public void create_desktop_window(int hwnd) {
        Log.i("wine", String.format("create desktop view %08x", new Object[]{Integer.valueOf(hwnd)}));
        TopView topView = new TopView(this, hwnd);

        setContentView(topView);
        this.progress_dialog.dismiss();


        wine_config_changed(getResources().getConfiguration().densityDpi);

        Toast.makeText(this, "开始传入文件", Toast.LENGTH_SHORT).show();
        GameIo.sefGame(this);
    }

    public void create_window(int hwnd, boolean opengl, int parent, float scale, int pid) {
        WineWindow win = get_window(hwnd);
        if (win == null) {
            win = new WineWindow(hwnd, get_window(parent), scale);
            win.create_window_groups();
            if (win.parent == this.desktop_window) {
                win.create_whole_view();
            }
        }
        if (opengl) {
            win.create_client_view();
        }
    }

    public void destroy_window(int hwnd) {
        WineWindow win = get_window(hwnd);
        if (win != null) {
            win.destroy();
        }
    }

    public void set_window_parent(int hwnd, int parent, float scale, int pid) {
        WineWindow win = get_window(hwnd);
        if (win != null) {
            win.set_parent(get_window(parent), scale);
            if (win.parent == this.desktop_window) {
                win.create_whole_view();
            }
        }
    }

    @TargetApi(24)
    public void set_cursor(int id, int width, int height, int hotspotx, int hotspoty, int[] bits) {
        Log.i("wine", String.format("set_cursor id %d size %dx%d hotspot %dx%d", new Object[]{Integer.valueOf(id), Integer.valueOf(width), Integer.valueOf(height), Integer.valueOf(hotspotx), Integer.valueOf(hotspoty)}));
        if (bits != null) {
            this.current_cursor = PointerIcon.create(Bitmap.createBitmap(bits, width, height, Bitmap.Config.ARGB_8888), (float) hotspotx, (float) hotspoty);
        } else {
            this.current_cursor = PointerIcon.getSystemIcon(this, id);
        }
    }

    public void window_pos_changed(int hwnd, int flags, int insert_after, int owner, int style, Rect window_rect, Rect client_rect, Rect visible_rect) {
        WineWindow win = get_window(hwnd);
        if (win != null) {
            win.pos_changed(flags, insert_after, owner, style, window_rect, client_rect, visible_rect);
        }
    }

    public void createDesktopWindow(final int hwnd) {
        runOnUiThread(new Runnable() {
            public void run() {
                WineActivity.this.create_desktop_window(hwnd);
            }
        });
    }

    public void createWindow(int hwnd, boolean opengl, int parent, float scale, int pid) {
        final int i = hwnd;
        final boolean z = opengl;
        final int i2 = parent;
        final float f = scale;
        final int i3 = pid;
        runOnUiThread(new Runnable() {
            public void run() {
                WineActivity.this.create_window(i, z, i2, f, i3);
            }
        });
    }

    public void destroyWindow(final int hwnd) {
        runOnUiThread(new Runnable() {
            public void run() {
                WineActivity.this.destroy_window(hwnd);
            }
        });
    }

    public void setParent(int hwnd, int parent, float scale, int pid) {
        final int i = hwnd;
        final int i2 = parent;
        final float f = scale;
        final int i3 = pid;
        runOnUiThread(new Runnable() {
            public void run() {
                WineActivity.this.set_window_parent(i, i2, f, i3);
            }
        });
    }

    public void setCursor(int id, int width, int height, int hotspotx, int hotspoty, int[] bits) {
        if (Build.VERSION.SDK_INT >= 24) {
            final int i = id;
            final int i2 = width;
            final int i3 = height;
            final int i4 = hotspotx;
            final int i5 = hotspoty;
            final int[] iArr = bits;
            runOnUiThread(new Runnable() {
                public void run() {
                    WineActivity.this.set_cursor(i, i2, i3, i4, i5, iArr);
                }
            });
        }
    }

    public void windowPosChanged(int hwnd, int flags, int insert_after, int owner, int style, int window_left, int window_top, int window_right, int window_bottom, int client_left, int client_top, int client_right, int client_bottom, int visible_left, int visible_top, int visible_right, int visible_bottom) {
        final Rect window_rect = new Rect(window_left, window_top, window_right, window_bottom);
        final Rect client_rect = new Rect(client_left, client_top, client_right, client_bottom);
        final Rect visible_rect = new Rect(visible_left, visible_top, visible_right, visible_bottom);
        final int i = hwnd;
        final int i2 = flags;
        final int i3 = insert_after;
        final int i4 = owner;
        final int i5 = style;
        runOnUiThread(new Runnable() {
            public void run() {
                WineActivity.this.window_pos_changed(i, i2, i3, i4, i5, window_rect, client_rect, visible_rect);
            }
        });
    }

}
