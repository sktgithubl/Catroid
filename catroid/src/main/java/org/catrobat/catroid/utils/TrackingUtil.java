/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2017 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * An additional term exception under section 7 of the GNU Affero
 * General Public License, version 3, is available at
 * http://developer.catrobat.org/license_additional_term
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.catrobat.catroid.utils;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Pair;

import org.catrobat.catroid.CatroidApplication;
import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.common.TrackingConstants;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Scene;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.bricks.Brick;
import org.catrobat.catroid.content.bricks.FormulaBrick;
import org.catrobat.catroid.ui.fragment.AddBrickFragment;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TrackingUtil implements Trackable {

	private static final String TAG = TrackingUtil.class.getSimpleName();

	private static Map<String, Long> timerMap = new HashMap<>();

	public void trackCreateProgram(String projectName, Boolean landscapeMode, boolean exampleProgram) {
		List<Pair<String, String>> trackingData = new ArrayList<>();
		trackingData.add(new Pair<>(TrackingConstants.PROGRAM_NAME, projectName));
		trackingData.add(new Pair<>(TrackingConstants.LANDSCAPE, String.valueOf(landscapeMode)));
		trackingData.add(new Pair<>(TrackingConstants.CREATE_EXAMPLE_PROGRAM, String.valueOf(exampleProgram)));
		createJsonAndLogCustomEvent(TrackingConstants.CREATE_PROGRAM, trackingData);
	}

	public void trackCreateObject(String newSpriteName, String spriteSource) {
		List<Pair<String, String>> objectTrackingData = createSceneTrackingList();
		objectTrackingData.add(new Pair<>(TrackingConstants.SPRITE_NAME, newSpriteName));
		objectTrackingData.add(new Pair<>(TrackingConstants.SOURCE, spriteSource));
		createJsonAndLogCustomEvent(TrackingConstants.CREATE_OBJECT, objectTrackingData);

		if (spriteSource.equals(TrackingConstants.POCKET_PAINT)
				&& timerMap.containsKey(TrackingConstants.SESSION_POCKET_PAINT_CREATE_OBJECT)) {
			List<Pair<String, String>> pocketPaintTrackingData = createSceneTrackingList();
			long time = stopTimer(TrackingConstants.SESSION_POCKET_PAINT_CREATE_OBJECT);
			pocketPaintTrackingData.add(new Pair<>(TrackingConstants.SESSION_DURATION_POCKET_PAINT, String.valueOf(time)));
			createJsonAndLogCustomEvent(TrackingConstants.SESSION_STOP_POCKET_PAINT_CREATE_OBJECT, pocketPaintTrackingData);
		}
	}

	public void trackStartWebSessionExplore() {
		List<Pair<String, String>> trackingData = new ArrayList<>();
		startTimer(TrackingConstants.SESSION_WEB_EXPLORE);
		createJsonAndLogCustomEvent(TrackingConstants.SESSION_START_WEB_EXPLORE, trackingData);
	}

	public void trackStopWebSessionExplore() {
		if (!timerMap.containsKey(TrackingConstants.SESSION_WEB_EXPLORE)) {
			return;
		}

		List<Pair<String, String>> trackingData = new ArrayList<>();
		long time = stopTimer(TrackingConstants.SESSION_WEB_EXPLORE);
		trackingData.add(new Pair<>(TrackingConstants.SESSION_DURATION_WEB, String.valueOf(time)));
		createJsonAndLogCustomEvent(TrackingConstants.SESSION_STOP_WEB_EXPLORE, trackingData);
	}

	public void trackStartWebSessionTutorial() {
		List<Pair<String, String>> trackingData = new ArrayList<>();
		startTimer(TrackingConstants.SESSION_WEB_TUTORIAL);
		createJsonAndLogCustomEvent(TrackingConstants.SESSION_START_WEB_TUTORIAL, trackingData);
	}

	public void trackStopWebSessionTutorial() {
		if (!timerMap.containsKey(TrackingConstants.SESSION_WEB_TUTORIAL)) {
			return;
		}

		List<Pair<String, String>> trackingData = new ArrayList<>();
		long time = stopTimer(TrackingConstants.SESSION_WEB_TUTORIAL);
		trackingData.add(new Pair<>(TrackingConstants.SESSION_DURATION_WEB, String.valueOf(time)));
		createJsonAndLogCustomEvent(TrackingConstants.SESSION_STOP_WEB_TUTORIAL, trackingData);
	}

	public void trackStartPocketPaintSessionCreateObject() {
		List<Pair<String, String>> trackingData = createSceneTrackingList();
		startTimer(TrackingConstants.SESSION_POCKET_PAINT_CREATE_OBJECT);
		createJsonAndLogCustomEvent(TrackingConstants.SESSION_START_POCKET_PAINT_CREATE_OBJECT, trackingData);
	}

	public void trackPocketPaintSessionLook(String timerId, String trackingMessage) {
		List<Pair<String, String>> trackingData = createSpriteTrackingList();
		startTimer(timerId);
		createJsonAndLogCustomEvent(trackingMessage, trackingData);
	}

	public void trackCreateLook(String lookName, String lookSource, String customEventMessage, String
			customEventMessageStop, String timerId) {
		List<Pair<String, String>> lookTrackingData = createSpriteTrackingList();
		lookTrackingData.add(new Pair<>(TrackingConstants.LOOK_NAME, lookName));
		lookTrackingData.add(new Pair<>(TrackingConstants.SOURCE, lookSource));
		createJsonAndLogCustomEvent(customEventMessage, lookTrackingData);

		if (lookSource.equals(TrackingConstants.POCKET_PAINT) && timerMap.containsKey(timerId)) {
			List<Pair<String, String>> pocketPaintTrackingData = createSpriteTrackingList();
			long time = stopTimer(timerId);
			pocketPaintTrackingData.add(new Pair<>(TrackingConstants.SESSION_DURATION_POCKET_PAINT, String.valueOf(time)));
			pocketPaintTrackingData.add(new Pair<>(TrackingConstants.LOOK_NAME, lookName));
			createJsonAndLogCustomEvent(customEventMessageStop, pocketPaintTrackingData);
		}
	}

	public void trackCreateSound(String soundName, String soundSource, long lengthMilliseconds) {
		List<Pair<String, String>> trackingData = createSpriteTrackingList();
		trackingData.add(new Pair<>(TrackingConstants.SOUND_NAME, soundName));
		trackingData.add(new Pair<>(TrackingConstants.SOURCE, soundSource));
		trackingData.add(new Pair<>(TrackingConstants.LENGTH, String.valueOf(lengthMilliseconds)));
		createJsonAndLogCustomEvent(TrackingConstants.CREATE_SOUND, trackingData);
	}

	public void trackAddBrick(Fragment addBrickFragment, Brick brickToBeAdded) {
		Bundle bundle = addBrickFragment.getArguments();
		if (bundle == null) {
			return;
		}

		String brickCategory = bundle.getString(AddBrickFragment.BUNDLE_ARGUMENTS_SELECTED_CATEGORY);
		String brickName = brickToBeAdded.getClass().getSimpleName();

		List<Pair<String, String>> trackingData = createSpriteTrackingList();
		trackingData.add(new Pair<>(TrackingConstants.BRICK_CATEGORY, brickCategory));
		trackingData.add(new Pair<>(TrackingConstants.BRICK_NAME, brickName));
		createJsonAndLogCustomEvent(TrackingConstants.ADD_BRICK, trackingData);
	}

	public void trackBrick(String brickName, String trackMessage) {
		List<Pair<String, String>> trackingData = createSpriteTrackingList();
		trackingData.add(new Pair<>(TrackingConstants.BRICK_NAME, brickName));
		createJsonAndLogCustomEvent(trackMessage, trackingData);
	}

	public void trackData(String name, String variableScope, String trackMessage) {
		List<Pair<String, String>> trackingData = createSpriteTrackingList();
		trackingData.add(new Pair<>(TrackingConstants.NAME, name));
		trackingData.add(new Pair<>(TrackingConstants.SCOPE, variableScope));
		createJsonAndLogCustomEvent(trackMessage, trackingData);
	}

	public void trackMenuButtonProject(String projectName, String trackMessage) {
		List<Pair<String, String>> trackingData = new ArrayList<>();
		trackingData.add(new Pair<>(TrackingConstants.PROGRAM_NAME, projectName));
		createJsonAndLogCustomEvent(trackMessage, trackingData);
	}

	public void trackProject(String name, String trackMessage) {
		List<Pair<String, String>> trackingData = new ArrayList<>();
		trackingData.add(new Pair<>(TrackingConstants.PROGRAM_NAME, name));
		createJsonAndLogCustomEvent(trackMessage, trackingData);
	}

	public void trackDeleteSprite(Sprite spriteToEdit) {
		List<Pair<String, String>> trackingData = createSpriteTrackingList();
		trackingData.add(new Pair<>(TrackingConstants.SPRITE_NAME, spriteToEdit.getName()));
		trackingData.add(new Pair<>(TrackingConstants.AMOUNT_BRICKS, String.valueOf(spriteToEdit.getNumberOfBricks())));
		trackingData.add(new Pair<>(TrackingConstants.AMOUNT_SCRIPTS, String.valueOf(spriteToEdit.getNumberOfScripts())));
		trackingData.add(new Pair<>(TrackingConstants.AMOUNT_LOOKS, String.valueOf(spriteToEdit.getLookDataList().size())));
		trackingData.add(new Pair<>(TrackingConstants.AMOUNT_SOUNDS, String.valueOf(spriteToEdit.getSoundList().size())));
		createJsonAndLogCustomEvent(TrackingConstants.DELETE_SPRITE, trackingData);
	}

	public void trackSprite(String name, String trackMessage) {
		List<Pair<String, String>> trackingData = createSceneTrackingList();
		trackingData.add(new Pair<>(TrackingConstants.NAME, name));
		createJsonAndLogCustomEvent(trackMessage, trackingData);
	}

	public void trackLook(String lookName, String trackMessage) {
		List<Pair<String, String>> trackingData = createSpriteTrackingList();
		trackingData.add(new Pair<>(TrackingConstants.LOOK_NAME, lookName));
		createJsonAndLogCustomEvent(trackMessage, trackingData);
	}

	public void trackSound(String soundName, String trackMessage) {
		List<Pair<String, String>> trackingData = createSpriteTrackingList();
		trackingData.add(new Pair<>(TrackingConstants.SOUND_NAME, soundName));
		createJsonAndLogCustomEvent(trackMessage, trackingData);
	}

	public void trackDropBrick(Brick draggedBrick) {
		List<Pair<String, String>> trackingData = createSpriteTrackingList();
		trackingData.add(new Pair<>(TrackingConstants.BRICK_NAME, draggedBrick.getClass().getSimpleName()));
		createJsonAndLogCustomEvent(TrackingConstants.DROP_BRICK, trackingData);
	}

	public void trackScene(String projectName, String sceneName, String trackMessage) {
		List<Pair<String, String>> trackingData = createSceneTrackingList();
		trackingData.add(new Pair<>(TrackingConstants.PROGRAM_NAME, projectName));
		trackingData.add(new Pair<>(TrackingConstants.SCENE_NAME, sceneName));
		createJsonAndLogCustomEvent(trackMessage, trackingData);
	}

	public void trackMerge(String firstProject, String secondProject) {
		List<Pair<String, String>> trackingData = new ArrayList<>();
		trackingData.add(new Pair<>(TrackingConstants.FIRST_PROGRAM_NAME, firstProject));
		trackingData.add(new Pair<>(TrackingConstants.SECOND_PROGRAM_NAME, secondProject));
		createJsonAndLogCustomEvent(TrackingConstants.MERGE_PROGRAMS, trackingData);
	}

	public void trackFormula(FormulaBrick formulaBrick, String brickField, String formula, String trackingMessage) {
		String brickName = formulaBrick.getClass().getSimpleName();

		List<Pair<String, String>> trackingData = createSpriteTrackingList();
		trackingData.add(new Pair<>(TrackingConstants.BRICK_NAME, brickName));
		trackingData.add(new Pair<>(TrackingConstants.BRICK_FIELD, brickField));
		trackingData.add(new Pair<>(TrackingConstants.FORMULA, formula));
		createJsonAndLogCustomEvent(trackingMessage, trackingData);
	}

	public void trackStartExecution() {
		List<Pair<String, String>> trackingData = createSceneTrackingList();
		startTimer(TrackingConstants.SESSION_PROGRAM_EXECUTION);
		createJsonAndLogCustomEvent(TrackingConstants.SESSION_START_PROGRAM_EXECUTION, trackingData);
	}

	public void trackStopExecution() {
		if (!timerMap.containsKey(TrackingConstants.SESSION_PROGRAM_EXECUTION)) {
			return;
		}

		String programName = ProjectManager.getInstance().getCurrentProject().getName();

		List<Pair<String, String>> trackingData = new ArrayList<>();
		trackingData.add(new Pair<>(TrackingConstants.PROGRAM_NAME, programName));
		long time = stopTimer(TrackingConstants.SESSION_PROGRAM_EXECUTION);
		trackingData.add(new Pair<>(TrackingConstants.SESSION_DURATION_PROGRAM_EXECUTION, String.valueOf(time)));
		createJsonAndLogCustomEvent(TrackingConstants.SESSION_STOP_PROGRAM_EXECUTION, trackingData);
	}

	public void trackBackpackSprite(String name, String trackingMessage) {
		List<Pair<String, String>> trackingData = createSceneTrackingList();
		trackingData.add(new Pair<>(TrackingConstants.SPRITE_NAME, name));
		createJsonAndLogCustomEvent(trackingMessage, trackingData);
	}

	public void trackBackpackScenes(String name, String trackingMessage) {
		List<Pair<String, String>> trackingData = createProjectTrackingList();
		trackingData.add(new Pair<>(TrackingConstants.SCENE_NAME, name));
		createJsonAndLogCustomEvent(trackingMessage, trackingData);
	}

	public void trackMergeScenes(String firstScene, String secondScene, String name) {
		String programName = ProjectManager.getInstance().getCurrentProject().getName();

		List<Pair<String, String>> trackingData = new ArrayList<>();
		trackingData.add(new Pair<>(TrackingConstants.PROGRAM_NAME, programName));
		trackingData.add(new Pair<>(TrackingConstants.FIRST_SCENE_NAME, firstScene));
		trackingData.add(new Pair<>(TrackingConstants.SECOND_SCENE_NAME, secondScene));
		trackingData.add(new Pair<>(TrackingConstants.NEW_SCENE_NAME, name));
		createJsonAndLogCustomEvent(TrackingConstants.MERGE_SCENES, trackingData);
	}

	public void trackBackpackBricks(List<Script> scriptsToAdd, int brickAmount, String groupName,
			String trackingMessage) {
		String scriptNames = "";
		for (int scriptPosition = 0; scriptPosition < scriptsToAdd.size(); scriptPosition++) {
			String scriptName = scriptsToAdd.get(scriptPosition).getClass().getSimpleName();
			if (scriptPosition < scriptsToAdd.size() - 1 && !scriptNames.isEmpty()) {
				scriptNames += ", " + scriptName;
			} else {
				scriptNames += scriptName;
			}
		}

		List<Pair<String, String>> trackingData = createSpriteTrackingList();
		trackingData.add(new Pair<>(TrackingConstants.AMOUNT_SCRIPTS, String.valueOf(scriptsToAdd.size())));
		trackingData.add(new Pair<>(TrackingConstants.SCRIPT_NAME, scriptNames));
		if (brickAmount != 0) {
			trackingData.add(new Pair<>(TrackingConstants.AMOUNT_BRICKS, String.valueOf(brickAmount)));
		}
		trackingData.add(new Pair<>(TrackingConstants.GROUP_NAME, groupName));
		createJsonAndLogCustomEvent(trackingMessage, trackingData);
	}

	public void trackUseTemplate(String templateName, boolean landscape) {
		List<Pair<String, String>> trackingData = new ArrayList<>();
		trackingData.add(new Pair<>(TrackingConstants.TEMPLATE_NAME, templateName));
		trackingData.add(new Pair<>(TrackingConstants.LANDSCAPE, String.valueOf(landscape)));
		createJsonAndLogCustomEvent(TrackingConstants.USE_TEMPLATE, trackingData);
	}

	public void trackApplyAccessibilityPreferences(String profileName, String settingName) {
		List<Pair<String, String>> trackingData = new ArrayList<>();
		trackingData.add(new Pair<>(TrackingConstants.ACTIVE_PROFILE, profileName));
		trackingData.add(new Pair<>(TrackingConstants.ACTIVE_SETTINGS, settingName));
		createJsonAndLogCustomEvent(TrackingConstants.APPLY_ACCESSIBILITY_SETTINGS, trackingData);
	}

	public void trackUseBrickHelp(Brick brick) {
		List<Pair<String, String>> trackingData = createSpriteTrackingList();
		trackingData.add(new Pair<>(TrackingConstants.BRICK_NAME, brick.getClass().getSimpleName()));
		createJsonAndLogCustomEvent(TrackingConstants.BRICK_HELP, trackingData);
	}

	public void trackCreateBroadcastMessage(String message) {
		List<Pair<String, String>> trackingData = createSpriteTrackingList();
		trackingData.add(new Pair<>(TrackingConstants.MESSAGE, message));
		createJsonAndLogCustomEvent(TrackingConstants.CREATE_BROADCAST_MESSAGE, trackingData);
	}

	public void trackSubmitProject(String programId) {
		List<Pair<String, String>> trackingData = new ArrayList<>();
		trackingData.add(new Pair<>(TrackingConstants.PROGRAM_ID, programId));
		createJsonAndLogCustomEvent(TrackingConstants.SUBMIT_PROGRAM, trackingData);
	}

	public void trackEnableHints(String enabled) {
		List<Pair<String, String>> trackingData = new ArrayList<>();
		trackingData.add(new Pair<>(TrackingConstants.ALLOW_HINTS, enabled));
		createJsonAndLogCustomEvent(TrackingConstants.HINTS_OPTION, trackingData);
	}

	public void trackLoginInitSessionEvent(Context context) {
		PreferenceManager.getDefaultSharedPreferences(context).edit().putLong(TrackingConstants.LOGIN_TIME, System.currentTimeMillis()).commit();

		//TODO replace bdsclient SDK
		Log.d(TAG, "TODO Track init session at " + System.currentTimeMillis()
				+ " with userid " + ProjectManager.getInstance().getUserID(context));
	}

	public void trackLogoutEndSessionEvent(Activity activity) {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
		long time = System.currentTimeMillis() - preferences.getLong(TrackingConstants.LOGIN_TIME, 0);
		preferences.edit().remove(TrackingConstants.LOGIN_TIME).commit();

		//TODO replace bdsclient SDK
		Log.d(TAG, "TODO Track end session at " + time + " with userid " + ProjectManager.getInstance().getUserID(activity));
	}

	private List<Pair<String, String>> createProjectTrackingList() {
		Project program = ProjectManager.getInstance().getCurrentProject();
		String programName = program != null ? program.getName() : TrackingConstants.NO_PROGRAM;

		List<Pair<String, String>> trackingData = new ArrayList<>();
		trackingData.add(new Pair<>(TrackingConstants.PROGRAM_NAME, programName));

		return trackingData;
	}

	private List<Pair<String, String>> createSceneTrackingList() {
		Scene scene = ProjectManager.getInstance().getCurrentScene();
		String sceneName = scene != null ? scene.getName() : TrackingConstants.NO_SCENE;

		List<Pair<String, String>> trackingData = createProjectTrackingList();
		trackingData.add(new Pair<>(TrackingConstants.SCENE_NAME, String.valueOf(sceneName)));

		return trackingData;
	}

	private List<Pair<String, String>> createSpriteTrackingList() {
		Sprite sprite = ProjectManager.getInstance().getCurrentSprite();
		String objectName = sprite != null ? sprite.getName() : TrackingConstants.NO_SPRITE;

		List<Pair<String, String>> trackingData = createSceneTrackingList();
		trackingData.add(new Pair<>(TrackingConstants.SPRITE_NAME, objectName));

		return trackingData;
	}

	private void createJsonAndLogCustomEvent(String trackingMessage, List<Pair<String, String>> trackingData) {

		JSONObject jsonObject = new JSONObject();
		try {
			for (Pair<String, String> data : trackingData) {
				jsonObject.put(data.first, data.second);
			}

			Context context = CatroidApplication.getAppContext();
			PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
			jsonObject.put(TrackingConstants.VERSION_CODE, packageInfo.versionCode);
		} catch (JSONException exception) {
			Log.e(TAG, "Could not serialize tracking data: " + exception.getMessage());
		} catch (NameNotFoundException exception) {
			Log.e(TAG, "Could not read versionCode to track: " + exception.getMessage());
		}

		logCustomEvent(trackingMessage, jsonObject);
	}

	private void logCustomEvent(String eventName, JSONObject jsonObject) {
		Context context = CatroidApplication.getAppContext();
		//TODO replace bdsclient SDK
		Log.d(TAG, "TODO Track custom event at " + System.currentTimeMillis()
				+ " with userid " + ProjectManager.getInstance().getUserID(context)
				+ " for event " + eventName
				+ " and custom data: " + jsonObject);
	}

	private void startTimer(String id) {
		timerMap.put(id, System.currentTimeMillis());
	}

	private long stopTimer(String id) {
		long time = System.currentTimeMillis() - timerMap.get(id);
		timerMap.remove(id);
		return time;
	}
}