package com.hades.hKtweaks.fragments.other;

import android.view.View;
import java.util.ArrayList;
import java.util.List;

import com.hades.hKtweaks.R;
import com.hades.hKtweaks.fragments.recyclerview.RecyclerViewFragment;
import com.hades.hKtweaks.utils.Utils;
import com.hades.hKtweaks.utils.root.RootUtils;
import com.hades.hKtweaks.views.recyclerview.ButtonView;
import com.hades.hKtweaks.views.recyclerview.CardView;
import com.hades.hKtweaks.views.recyclerview.DescriptionView;
import com.hades.hKtweaks.views.recyclerview.RecyclerViewItem;
import com.hades.hKtweaks.views.recyclerview.SeekBarView;
import com.hades.hKtweaks.views.recyclerview.SwitchView;

public class NetworkFragment extends RecyclerViewFragment {

    private int mDownloadLimitMbps = 10;
    private static final String WIFI_IFACE = "wlan0";

    @Override
    public int getSpanCount() { return 1; }

    @Override
    protected void addItems(List<RecyclerViewItem> items) {
        networkInfoInit(items);
        wifiSpeedInit(items);
        connectedDevicesInit(items);
    }

    private void networkInfoInit(List<RecyclerViewItem> items) {
        CardView card = new CardView(getActivity());
        card.setTitle(getString(R.string.network_info));

        String ip = RootUtils.runCommand(
            "ip addr show " + WIFI_IFACE + " 2>/dev/null | grep 'inet ' | awk '{print $2}' | cut -d/ -f1");
        String gw = RootUtils.runCommand(
            "ip route 2>/dev/null | grep default | awk '{print $3}' | head -1");
        String dns = RootUtils.runCommand("getprop net.dns1 2>/dev/null");

        DescriptionView ipView = new DescriptionView();
        ipView.setTitle(getString(R.string.ip_address));
        ipView.setSummary((ip == null || ip.trim().isEmpty())
            ? getString(R.string.unknown) : ip.trim());
        card.addItem(ipView);

        if (gw != null && !gw.trim().isEmpty()) {
            DescriptionView gwView = new DescriptionView();
            gwView.setTitle(getString(R.string.gateway));
            gwView.setSummary(gw.trim());
            card.addItem(gwView);
        }

        if (dns != null && !dns.trim().isEmpty()) {
            DescriptionView dnsView = new DescriptionView();
            dnsView.setTitle("DNS");
            dnsView.setSummary(dns.trim());
            card.addItem(dnsView);
        }

        items.add(card);
    }

    private void wifiSpeedInit(List<RecyclerViewItem> items) {
        CardView card = new CardView(getActivity());
        card.setTitle(getString(R.string.wifi_speed));

        DescriptionView desc = new DescriptionView();
        desc.setSummary(getString(R.string.wifi_speed_summary));
        card.addItem(desc);

        SeekBarView dlSeek = new SeekBarView();
        dlSeek.setTitle(getString(R.string.max_download));
        dlSeek.setUnit(" Mbps");
        dlSeek.setMax(99);
        dlSeek.setOffset(1);
        dlSeek.setProgress(mDownloadLimitMbps - 1);
        dlSeek.setOnSeekBarListener(new SeekBarView.OnSeekBarListener() {
            @Override
            public void onStop(SeekBarView seekBarView, int position, String value) {
                mDownloadLimitMbps = position + 1;
            }
            @Override
            public void onMove(SeekBarView seekBarView, int position, String value) {}
        });
        card.addItem(dlSeek);

        ButtonView applyBtn = new ButtonView();
        applyBtn.setText(getString(R.string.apply_limits));
        applyBtn.setOnClickListener(v -> {
            String cmd = "tc qdisc del dev " + WIFI_IFACE + " root 2>/dev/null; "
                + "tc qdisc add dev " + WIFI_IFACE + " root tbf rate "
                + mDownloadLimitMbps + "mbit burst 32kbit latency 400ms 2>/dev/null";
            RootUtils.runCommand(cmd);
            Utils.toast(getString(R.string.apply_limits) + ": "
                + mDownloadLimitMbps + " Mbps", getActivity());
        });
        card.addItem(applyBtn);

        ButtonView resetBtn = new ButtonView();
        resetBtn.setText(getString(R.string.reset_limits));
        resetBtn.setOnClickListener(v -> {
            RootUtils.runCommand("tc qdisc del dev " + WIFI_IFACE + " root 2>/dev/null");
            Utils.toast(getString(R.string.unlimited), getActivity());
        });
        card.addItem(resetBtn);

        items.add(card);
    }

    private void connectedDevicesInit(List<RecyclerViewItem> items) {
        CardView card = new CardView(getActivity());
        card.setTitle(getString(R.string.connected_devices));

        DescriptionView desc = new DescriptionView();
        desc.setSummary(getString(R.string.connected_devices_summary));
        card.addItem(desc);

        String arpOut = RootUtils.runCommand("cat /proc/net/arp 2>/dev/null");
        List<String[]> devices = parseArp(arpOut);

        if (devices.isEmpty()) {
            DescriptionView empty = new DescriptionView();
            empty.setSummary(getString(R.string.no_devices));
            card.addItem(empty);
        } else {
            for (String[] dev : devices) {
                String devIp  = dev[0];
                String devMac = dev[1];
                boolean blocked = !RootUtils.runCommand(
                    "iptables -L FORWARD -n 2>/dev/null | grep -w '" + devIp + "'").isEmpty();

                SwitchView sw = new SwitchView();
                sw.setTitle(devIp);
                sw.setSummaryOn(getString(R.string.blocked)   + "  [" + devMac + "]");
                sw.setSummaryOff(getString(R.string.active)   + "  [" + devMac + "]");
                sw.setChecked(blocked);
                sw.addOnSwitchListener((switchView, checked) -> {
                    if (checked) {
                        RootUtils.runCommand("iptables -I FORWARD -s " + devIp + " -j DROP 2>/dev/null");
                        RootUtils.runCommand("iptables -I FORWARD -d " + devIp + " -j DROP 2>/dev/null");
                    } else {
                        RootUtils.runCommand("iptables -D FORWARD -s " + devIp + " -j DROP 2>/dev/null");
                        RootUtils.runCommand("iptables -D FORWARD -d " + devIp + " -j DROP 2>/dev/null");
                    }
                });
                card.addItem(sw);
            }
        }

        ButtonView refreshBtn = new ButtonView();
        refreshBtn.setText(getString(R.string.scan_devices));
        refreshBtn.setOnClickListener(v ->
            getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.content_frame, new NetworkFragment())
                .commitAllowingStateLoss());
        card.addItem(refreshBtn);

        items.add(card);
    }

    private List<String[]> parseArp(String arp) {
        List<String[]> list = new ArrayList<>();
        if (arp == null || arp.isEmpty()) return list;
        for (String line : arp.split("\n")) {
            line = line.trim();
            if (line.isEmpty()) continue;
            String[] p = line.split("\\s+");
            if (p.length >= 4 && !p[2].equals("0x0") && !p[3].equals("00:00:00:00:00:00")) {
                list.add(new String[]{p[0], p[3]});
            }
        }
        return list;
    }
}
