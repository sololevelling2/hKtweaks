package com.hades.hKtweaks.fragments.other;

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

public class GameOptimizationFragment extends RecyclerViewFragment {

    private static final String[] GOVERNORS = {
        "performance", "schedutil", "interactive", "ondemand", "conservative"
    };

    @Override
    public int getSpanCount() { return 1; }

    @Override
    protected void addItems(List<RecyclerViewItem> items) {
        gameModeInit(items);
        cpuGovInit(items);
        gpuBoostInit(items);
        networkPriorityInit(items);
        thermalOverrideInit(items);
        bgAppsInit(items);
    }

    private void gameModeInit(List<RecyclerViewItem> items) {
        CardView card = new CardView(getActivity());
        card.setTitle(getString(R.string.game_mode));

        DescriptionView desc = new DescriptionView();
        desc.setSummary(getString(R.string.game_mode_summary));
        card.addItem(desc);

        String curGov = RootUtils.runCommand(
            "cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor 2>/dev/null").trim();
        SwitchView gameMode = new SwitchView();
        gameMode.setTitle(getString(R.string.game_mode));
        gameMode.setSummaryOn(getString(R.string.game_mode_on));
        gameMode.setSummaryOff(getString(R.string.game_mode_off));
        gameMode.setChecked("performance".equals(curGov));
        gameMode.addOnSwitchListener((switchView, checked) -> {
            if (checked) {
                applyGameMode();
                Utils.toast(getString(R.string.game_mode_on), getActivity());
            } else {
                restoreDefault();
                Utils.toast(getString(R.string.game_mode_off), getActivity());
            }
        });
        card.addItem(gameMode);
        items.add(card);
    }

    private void cpuGovInit(List<RecyclerViewItem> items) {
        CardView card = new CardView(getActivity());
        card.setTitle(getString(R.string.game_cpu_governor));

        String cur = RootUtils.runCommand(
            "cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor 2>/dev/null").trim();
        int idx = 0;
        for (int i = 0; i < GOVERNORS.length; i++) {
            if (GOVERNORS[i].equals(cur)) { idx = i; break; }
        }

        SelectView govSel = new SelectView();
        govSel.setTitle(getString(R.string.game_cpu_governor));
        govSel.setSummary(getString(R.string.game_cpu_governor_summary));
        govSel.setItems(Arrays.asList(GOVERNORS));
        govSel.setItem(idx);
        govSel.setOnItemSelected((selectView, position, item) ->
            RootUtils.runCommand(
                "for f in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; "
                + "do echo " + GOVERNORS[position] + " > $f 2>/dev/null; done"));
        card.addItem(govSel);
        items.add(card);
    }

    private void gpuBoostInit(List<RecyclerViewItem> items) {
        CardView card = new CardView(getActivity());
        card.setTitle(getString(R.string.game_gpu_boost));

        String gpuMaxPath = "/sys/kernel/gpu/gpu_max_clock";
        String gpuMinPath = "/sys/kernel/gpu/gpu_min_clock";
        String maxFreq = RootUtils.runCommand("cat " + gpuMaxPath + " 2>/dev/null").trim();
        String minFreq = RootUtils.runCommand("cat " + gpuMinPath + " 2>/dev/null").trim();
        String curMin  = RootUtils.runCommand("cat " + gpuMinPath + " 2>/dev/null").trim();

        DescriptionView desc = new DescriptionView();
        desc.setSummary(getString(R.string.game_gpu_boost_summary));
        card.addItem(desc);

        SwitchView gpuBoost = new SwitchView();
        gpuBoost.setTitle(getString(R.string.game_gpu_boost));
        gpuBoost.setSummaryOn(getString(R.string.game_gpu_boost));
        gpuBoost.setSummaryOff(getString(R.string.game_gpu_boost_summary));
        gpuBoost.setChecked(!maxFreq.isEmpty() && maxFreq.equals(curMin));
        gpuBoost.addOnSwitchListener((switchView, checked) -> {
            if (checked && !maxFreq.isEmpty()) {
                RootUtils.runCommand("echo " + maxFreq + " > " + gpuMinPath + " 2>/dev/null");
            } else if (!minFreq.isEmpty()) {
                RootUtils.runCommand("echo " + minFreq + " > " + gpuMinPath + " 2>/dev/null");
            }
        });
        card.addItem(gpuBoost);
        items.add(card);
    }

    private void networkPriorityInit(List<RecyclerViewItem> items) {
        CardView card = new CardView(getActivity());
        card.setTitle(getString(R.string.game_network_priority));

        DescriptionView desc = new DescriptionView();
        desc.setSummary(getString(R.string.game_network_priority_summary));
        card.addItem(desc);

        SwitchView netPri = new SwitchView();
        netPri.setTitle(getString(R.string.game_network_priority));
        netPri.setSummaryOn(getString(R.string.game_network_priority));
        netPri.setSummaryOff(getString(R.string.game_network_priority_summary));
        netPri.setChecked(false);
        netPri.addOnSwitchListener((switchView, checked) -> {
            if (checked) {
                RootUtils.runCommand("sysctl -w net.ipv4.tcp_low_latency=1 2>/dev/null");
                RootUtils.runCommand("sysctl -w net.core.netdev_max_backlog=2000 2>/dev/null");
                RootUtils.runCommand("sysctl -w net.ipv4.tcp_fin_timeout=15 2>/dev/null");
            } else {
                RootUtils.runCommand("sysctl -w net.ipv4.tcp_low_latency=0 2>/dev/null");
                RootUtils.runCommand("sysctl -w net.core.netdev_max_backlog=1000 2>/dev/null");
                RootUtils.runCommand("sysctl -w net.ipv4.tcp_fin_timeout=60 2>/dev/null");
            }
        });
        card.addItem(netPri);
        items.add(card);
    }

    private void thermalOverrideInit(List<RecyclerViewItem> items) {
        CardView card = new CardView(getActivity());
        card.setTitle(getString(R.string.game_thermal_override));

        DescriptionView desc = new DescriptionView();
        desc.setSummary(getString(R.string.game_thermal_override_summary));
        card.addItem(desc);

        boolean overridden = "1".equals(
            RootUtils.runCommand("getprop persist.vendor.disable.thermal.control 2>/dev/null").trim());
        SwitchView thermal = new SwitchView();
        thermal.setTitle(getString(R.string.game_thermal_override));
        thermal.setSummaryOn(getString(R.string.game_thermal_override));
        thermal.setSummaryOff(getString(R.string.game_thermal_override_summary));
        thermal.setChecked(overridden);
        thermal.addOnSwitchListener((switchView, checked) ->
            RootUtils.runCommand("setprop persist.vendor.disable.thermal.control "
                + (checked ? "1" : "0") + " 2>/dev/null"));
        card.addItem(thermal);
        items.add(card);
    }

    private void bgAppsInit(List<RecyclerViewItem> items) {
        CardView card = new CardView(getActivity());
        card.setTitle(getString(R.string.game_disable_bg));

        DescriptionView desc = new DescriptionView();
        desc.setSummary(getString(R.string.game_disable_bg_summary));
        card.addItem(desc);

        ButtonView killBtn = new ButtonView();
        killBtn.setTitle(getString(R.string.game_disable_bg));
        killBtn.setOnItemClickListener(item -> {
            RootUtils.runCommand("am kill-all 2>/dev/null");
            Utils.toast(getString(R.string.game_disable_bg_summary), getActivity());
        });
        card.addItem(killBtn);
        items.add(card);
    }

    private void applyGameMode() {
        RootUtils.runCommand(
            "for f in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; "
            + "do echo performance > $f 2>/dev/null; done");
        RootUtils.runCommand(
            "cat /sys/kernel/gpu/gpu_max_clock > /sys/kernel/gpu/gpu_min_clock 2>/dev/null");
        RootUtils.runCommand("sysctl -w net.ipv4.tcp_low_latency=1 2>/dev/null");
        RootUtils.runCommand("am kill-all 2>/dev/null");
    }

    private void restoreDefault() {
        RootUtils.runCommand(
            "for f in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; "
            + "do echo schedutil > $f 2>/dev/null; done");
        RootUtils.runCommand("sysctl -w net.ipv4.tcp_low_latency=0 2>/dev/null");
    }
}
