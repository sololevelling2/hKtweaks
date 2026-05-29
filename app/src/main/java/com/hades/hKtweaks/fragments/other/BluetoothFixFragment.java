package com.hades.hKtweaks.fragments.other;

import android.view.View;
import java.util.Arrays;
import java.util.List;

import com.hades.hKtweaks.R;
import com.hades.hKtweaks.fragments.recyclerview.RecyclerViewFragment;
import com.hades.hKtweaks.utils.Utils;
import com.hades.hKtweaks.utils.root.RootUtils;
import com.hades.hKtweaks.views.recyclerview.ButtonView;
import com.hades.hKtweaks.views.recyclerview.CardView;
import com.hades.hKtweaks.views.recyclerview.DescriptionView;
import com.hades.hKtweaks.views.recyclerview.RecyclerViewItem;
import com.hades.hKtweaks.views.recyclerview.SelectView;
import com.hades.hKtweaks.views.recyclerview.SwitchView;

/**
 * BluetoothFixFragment — targeted fixes for BT HID issues on Samsung Exynos / One UI 6.1.1.
 * Addresses keyboards and input devices losing connection after reconnect.
 */
public class BluetoothFixFragment extends RecyclerViewFragment {

    @Override
    public int getSpanCount() { return 1; }

    @Override
    protected void addItems(List<RecyclerViewItem> items) {
        btResetInit(items);
        btKeyboardFixInit(items);
        btCoexistenceInit(items);
        btSnoopInit(items);
    }

    private void btResetInit(List<RecyclerViewItem> items) {
        CardView card = new CardView(getActivity());
        card.setTitle(getString(R.string.bt_reset));

        DescriptionView desc = new DescriptionView();
        desc.setSummary(getString(R.string.bt_reset_summary));
        card.addItem(desc);

        ButtonView resetBtn = new ButtonView();
        resetBtn.setText(getString(R.string.bt_reset));
        resetBtn.setOnClickListener(v -> {
            Utils.toast(getString(R.string.bt_service_restarting), getActivity());
            RootUtils.runCommand("svc bluetooth disable 2>/dev/null");
            RootUtils.runCommand("sleep 1");
            RootUtils.runCommand("svc bluetooth enable 2>/dev/null");
            Utils.toast(getString(R.string.bt_fix_applied), getActivity());
        });
        card.addItem(resetBtn);

        ButtonView clearHidBtn = new ButtonView();
        clearHidBtn.setText(getString(R.string.bt_clear_hid));
        clearHidBtn.setOnClickListener(v -> {
            Utils.toast(getString(R.string.bt_service_restarting), getActivity());
            RootUtils.runCommand("svc bluetooth disable 2>/dev/null");
            RootUtils.runCommand("sleep 1");
            RootUtils.runCommand("rm -f /data/misc/bluedroid/bt_config.conf 2>/dev/null");
            RootUtils.runCommand("rm -f /data/misc/bluedroid/bt_config.bak 2>/dev/null");
            RootUtils.runCommand("sleep 1");
            RootUtils.runCommand("svc bluetooth enable 2>/dev/null");
            Utils.toast(getString(R.string.bt_fix_applied), getActivity());
        });
        card.addItem(clearHidBtn);

        items.add(card);
    }

    private void btKeyboardFixInit(List<RecyclerViewItem> items) {
        CardView card = new CardView(getActivity());
        card.setTitle(getString(R.string.bt_fix_keyboard));

        DescriptionView desc = new DescriptionView();
        desc.setSummary(getString(R.string.bt_fix_keyboard_summary));
        card.addItem(desc);

        ButtonView fixBtn = new ButtonView();
        fixBtn.setText(getString(R.string.bt_fix_keyboard));
        fixBtn.setOnClickListener(v -> {
            RootUtils.runCommand(
                "setprop bluetooth.profile.hid.host.enabled true 2>/dev/null");
            RootUtils.runCommand(
                "settings put secure bluetooth_hfp_client_audio_channel_mask 2 2>/dev/null");
            RootUtils.runCommand(
                "rm -f /data/misc/bluetooth/hiddescriptors.bin 2>/dev/null");
            RootUtils.runCommand("svc bluetooth disable 2>/dev/null");
            RootUtils.runCommand("sleep 2");
            RootUtils.runCommand("svc bluetooth enable 2>/dev/null");
            Utils.toast(getString(R.string.bt_fix_applied), getActivity());
        });
        card.addItem(fixBtn);

        items.add(card);
    }

    private void btCoexistenceInit(List<RecyclerViewItem> items) {
        CardView card = new CardView(getActivity());
        card.setTitle(getString(R.string.bt_coexistence));

        DescriptionView desc = new DescriptionView();
        desc.setSummary(getString(R.string.bt_coexistence_summary));
        card.addItem(desc);

        String[] modes = {"Disabled (less interference)", "Enabled", "Sense (auto)"};
        String cur = RootUtils.runCommand(
            "getprop net.bt.nap.disabled 2>/dev/null").trim();
        int idx = "0".equals(cur) ? 1 : 0;

        SelectView sel = new SelectView();
        sel.setTitle(getString(R.string.bt_coexistence));
        sel.setItems(Arrays.asList(modes));
        sel.setItem(idx);
        sel.setOnItemSelected((selectView, position, it) -> {
            if (position == 0) {
                RootUtils.runCommand("setprop net.bt.nap.disabled 1 2>/dev/null");
                RootUtils.runCommand(
                    "echo 0 > /sys/module/wlan_sdio/parameters/bt_coex_active 2>/dev/null");
            } else if (position == 1) {
                RootUtils.runCommand("setprop net.bt.nap.disabled 0 2>/dev/null");
                RootUtils.runCommand(
                    "echo 1 > /sys/module/wlan_sdio/parameters/bt_coex_active 2>/dev/null");
            } else {
                RootUtils.runCommand("setprop net.bt.nap.disabled 0 2>/dev/null");
                RootUtils.runCommand(
                    "echo 2 > /sys/module/wlan_sdio/parameters/bt_coex_active 2>/dev/null");
            }
        });
        card.addItem(sel);

        items.add(card);
    }

    private void btSnoopInit(List<RecyclerViewItem> items) {
        CardView card = new CardView(getActivity());
        card.setTitle(getString(R.string.bt_hci_snoop));

        DescriptionView desc = new DescriptionView();
        desc.setSummary(getString(R.string.bt_hci_snoop_summary));
        card.addItem(desc);

        boolean snoopOn = "true".equals(
            RootUtils.runCommand(
                "getprop persist.bluetooth.btsnoopenable 2>/dev/null").trim());
        SwitchView sw = new SwitchView();
        sw.setTitle(getString(R.string.bt_hci_snoop));
        sw.setSummaryOn(getString(R.string.bt_hci_snoop));
        sw.setSummaryOff(getString(R.string.bt_hci_snoop_summary));
        sw.setChecked(snoopOn);
        sw.addOnSwitchListener((switchView, checked) ->
            RootUtils.runCommand("setprop persist.bluetooth.btsnoopenable "
                + (checked ? "true" : "false") + " 2>/dev/null"));
        card.addItem(sw);

        items.add(card);
    }
}
