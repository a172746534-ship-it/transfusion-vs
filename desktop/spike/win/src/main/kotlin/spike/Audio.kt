package spike

import com.sun.jna.Memory
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.Guid
import com.sun.jna.platform.win32.Ole32
import com.sun.jna.ptr.FloatByReference
import com.sun.jna.ptr.IntByReference
import com.sun.jna.ptr.PointerByReference
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import kotlin.concurrent.thread

/** Core Audio through COM vtables: the default render endpoint's volume and mute, and our own session. */
object Audio {
    private val CLSID_MMDeviceEnumerator = Guid.GUID.fromString("{BCDE0395-E52F-467C-8E3D-C4579291692E}")
    private val IID_IMMDeviceEnumerator = Guid.GUID.fromString("{A95664D2-9614-4F35-A746-DE8DB63617E6}")
    private val IID_IAudioEndpointVolume = Guid.GUID.fromString("{5CDF2C82-841E-4546-9722-0CF74078229A}")
    private val IID_IAudioSessionManager2 = Guid.GUID.fromString("{77AA99A0-1BD6-484F-8BC7-2C654C9A9B6F}")
    private val IID_IAudioSessionControl2 = Guid.GUID.fromString("{BFB7FF88-7239-4FC9-8FA2-07C950BE9C6D}")
    private val IID_ISimpleAudioVolume = Guid.GUID.fromString("{87CE5498-68D6-44E5-9215-6DA47EF883D8}")
    private val PKEY_FriendlyName = Guid.GUID.fromString("{A45C254E-DF1C-4EFD-8020-67D146A850E0}") to 14
    private val PKEY_FormFactor = Guid.GUID.fromString("{1DA5D803-D492-4EDD-8C23-E0C0FFEE7F0E}") to 0
    private const val CLSCTX_ALL = 0x17

    /** Runs the whole set-and-restore once, on its own COM thread, and logs every value. */
    fun spike(volumeSetting: Float = 0.8f) = thread(name = "audio") {
        runCatching { run(volumeSetting) }.onFailure { Log.i("AUDIO failed: $it") }
    }.join()

    private fun prop(store: Com, key: Pair<Guid.GUID, Int>): String {
        val pk = Memory(20).also { m -> m.write(0, guidMemory(key.first).getByteArray(0, 16), 0, 16); m.setInt(16, key.second) }
        val pv = Memory(24).also { it.clear() }
        if (store.call(5, pk, pv) != 0) return "?"
        return when (pv.getShort(0).toInt()) {
            31 -> pv.getPointer(8).getWideString(0)
            19 -> pv.getInt(8).toString()
            else -> "vt=${pv.getShort(0)}"
        }
    }

    private fun run(setting: Float) {
        Log.i("AUDIO CoInitializeEx=${Ole32.INSTANCE.CoInitializeEx(null, 0)}")
        val enumOut = PointerByReference()
        val hr = Ole32.INSTANCE.CoCreateInstance(CLSID_MMDeviceEnumerator, null, CLSCTX_ALL, IID_IMMDeviceEnumerator, enumOut)
        Log.i("AUDIO CoCreateInstance(MMDeviceEnumerator)=0x${Integer.toHexString(hr.toInt())}")
        if (hr.toInt() != 0) return
        val enumerator = Com(enumOut.value)
        // Every render endpoint, any state.
        val coll = PointerByReference()
        if (enumerator.call(3, 0, 0xF, coll) == 0) {
            val c = Com(coll.value)
            val count = IntByReference().also { c.call(3, it) }.value
            for (i in 0 until count) {
                val dev = PointerByReference()
                c.call(4, i, dev)
                val d = Com(dev.value)
                val state = IntByReference().also { d.call(6, it) }.value
                val store = PointerByReference()
                val name = if (d.call(4, 0, store) == 0) Com(store.value).let { "${prop(it, PKEY_FriendlyName)} formFactor=${prop(it, PKEY_FormFactor)}" } else "?"
                Log.i("AUDIO endpoint[$i] state=$state $name")
            }
            Log.i("AUDIO endpoints=$count")
        }
        val devOut = PointerByReference()
        val dhr = enumerator.call(4, 0, 0, devOut) // eRender, eConsole
        Log.i("AUDIO GetDefaultAudioEndpoint=0x${Integer.toHexString(dhr)}")
        if (dhr != 0) return
        val device = Com(devOut.value)
        val idOut = PointerByReference()
        device.call(5, idOut)
        val deviceId = idOut.value?.getWideString(0)
        val epvOut = PointerByReference()
        Log.i("AUDIO Activate(IAudioEndpointVolume)=0x${Integer.toHexString(device.call(3, IID_IAudioEndpointVolume, CLSCTX_ALL, null, epvOut))} device=$deviceId")
        val epv = Com(epvOut.value)
        val vol = FloatByReference().also { epv.call(9, it) }.value
        val mute = IntByReference().also { epv.call(15, it) }.value
        Log.i("AUDIO before: volume=$vol mute=$mute (restore record: device=$deviceId volume=$vol mute=$mute)")
        // While sounding: unmute and raise to max(current, setting).
        epv.call(14, 0, null)
        val target = maxOf(vol, setting)
        epv.call(7, target, null)
        Log.i("AUDIO raised: volume=${FloatByReference().also { epv.call(9, it) }.value} mute=${IntByReference().also { epv.call(15, it) }.value}")
        // Play our sound, then find our own session (it exists only while a stream is open).
        val clip: Clip? = runCatching {
            val stream = AudioSystem.getAudioInputStream(javaClass.getResource("/raw/alarm_tone.wav"))
            AudioSystem.getClip().also { it.open(stream); it.loop(Clip.LOOP_CONTINUOUSLY) }
        }.onFailure { Log.i("AUDIO clip failed: $it") }.getOrNull()
        Thread.sleep(800)
        Log.i("AUDIO clip running=${clip?.isRunning} mixer=${runCatching { AudioSystem.getMixerInfo().joinToString { it.name } }.getOrNull()}")
        val smOut = PointerByReference()
        if (device.call(3, IID_IAudioSessionManager2, CLSCTX_ALL, null, smOut) == 0) {
            val sm = Com(smOut.value)
            val seOut = PointerByReference()
            sm.call(5, seOut)
            val se = Com(seOut.value)
            val n = IntByReference().also { se.call(3, it) }.value
            val me = ProcessHandle.current().pid().toInt()
            for (i in 0 until n) {
                val ctlOut = PointerByReference()
                se.call(4, i, ctlOut)
                val ctl2 = Com(ctlOut.value).qi(IID_IAudioSessionControl2) ?: continue
                val pid = IntByReference().also { ctl2.call(14, it) }.value
                if (pid != me) continue
                val sav = ctl2.qi(IID_ISimpleAudioVolume)
                val sv = FloatByReference().also { sav?.call(4, it) }.value
                val smute = IntByReference().also { sav?.call(6, it) }.value
                sav?.call(3, 1.0f, null)
                sav?.call(5, 0, null)
                val duck = ctl2.call(16, 1)
                Log.i("AUDIO own session: before volume=$sv mute=$smute -> 1.0/0; SetDuckingPreference=0x${Integer.toHexString(duck)}")
            }
            Log.i("AUDIO sessions=$n")
        }
        clip?.stop()
        clip?.close()
        // Restore only what hasn't changed since we set it.
        val nowVol = FloatByReference().also { epv.call(9, it) }.value
        if (kotlin.math.abs(nowVol - target) < 0.001f) epv.call(7, vol, null)
        if (IntByReference().also { epv.call(15, it) }.value == 0) epv.call(14, mute, null)
        Log.i("AUDIO restored: volume=${FloatByReference().also { epv.call(9, it) }.value} mute=${IntByReference().also { epv.call(15, it) }.value}")
    }

    @Suppress("unused") private val keep = Pointer.NULL
}
